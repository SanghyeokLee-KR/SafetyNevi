package com.inha.pro.safetynevi.service.crawling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import com.inha.pro.safetynevi.infra.lock.SchedulerLockInitializer;
import com.inha.pro.safetynevi.infra.lock.SchedulerLockService;
import com.inha.pro.safetynevi.service.ai.AiClientService;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;

// 긴급재난문자 공식 API 1분마다 긁어서 신규 저장 + AI 위험판정시 지도에 영역 생성 (전엔 네이버 크롤링이었음)
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final DisasterMessageRepository disasterMessageRepository;
    private final DisasterService disasterService;
    private final AiClientService aiClientService;
    private final SchedulerLockService lockService;
    private final MessageBroadcaster messageBroadcaster;   // 지도 사이드바 실시간 피드 push (단일=직접 STOMP, 운영=Kafka)

    // 키 미설정 시에도 앱은 기동되도록 빈 기본값(이 경우 API 호출은 graceful 실패)
    @Value("${api.disaster.serviceKey:}")
    private String disasterServiceKey;

    // 재난안전데이터공유플랫폼 - 긴급재난문자 (data.go.kr 15134001)
    private static final String DISASTER_API_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247";

    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 외부 API가 느릴 때 스케줄러 스레드가 무한 대기하지 않도록 타임아웃
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    // ===== 적응형 폴링 + 분산 락 =====
    // 활동량 기반 간격: 신규 재난문자가 잡히면 최단(20s)으로, 잠잠하면 단계적으로 늘려(최대 120s) 비용↓
    private static final long[] BACKOFF_MS = {20_000L, 30_000L, 45_000L, 60_000L, 120_000L};
    private volatile int backoffStep = 0;
    private volatile long nextFetchAt = 0L;

    // 이 API는 오래된순 정렬 + totalCount 없음 → 마지막(=최신) 페이지를 찾아 캐시하고 그쪽부터 수집한다.
    private static final int PAGE_SIZE = 20;     // 페이지당 행 수
    private static final int RECENT_PAGES = 3;   // 최신 쪽에서 긁어올 페이지 수
    private volatile int latestPage = 0;         // 캐시된 마지막 페이지 (0=미탐색)

    // 20초마다 깨어나되 (1) 적응형 백오프, (2) 분산 락으로 실제 수집 시점을 제어한다.
    // 여러 인스턴스가 떠도 락을 잡은 한 곳만 외부 API를 호출 → 호출량·일일한도·중복저장 방지.
    @Scheduled(fixedDelay = 20_000)
    public void scheduledPoll() {
        if (disasterServiceKey == null || disasterServiceKey.isBlank()) return; // 키 없으면 동작 안 함
        if (System.currentTimeMillis() < nextFetchAt) return;                    // 백오프 대기 중
        if (!lockService.tryAcquire(SchedulerLockInitializer.DISASTER_CRAWL, Duration.ofSeconds(50))) {
            adjustBackoff(false);   // 다른 인스턴스가 수집 중 → 로컬도 잠시 대기
            return;
        }
        try {
            int saved = fetchDisasterMessages();
            adjustBackoff(saved > 0);   // 새 메시지 있으면 빠르게, 없으면 점점 느리게
        } finally {
            lockService.release(SchedulerLockInitializer.DISASTER_CRAWL);
        }
    }

    private void adjustBackoff(boolean active) {
        backoffStep = active ? 0 : Math.min(backoffStep + 1, BACKOFF_MS.length - 1);
        nextFetchAt = System.currentTimeMillis() + BACKOFF_MS[backoffStep];
    }

    // 공식 API 조회 → 신규 저장 + (firstRun 아닐 때) 위험 판정. 저장 건수를 반환.
    // 이 API는 오래된순 정렬이라 마지막(=최신) 페이지부터 거꾸로 몇 페이지를 긁어 최근 메시지를 모은다.
    // 외부 I/O(API·AI) 동안 DB 커넥션을 잡지 않으려 메서드 트랜잭션은 두지 않음.
    public int fetchDisasterMessages() {
        if (disasterServiceKey == null || disasterServiceKey.isBlank()) return 0;

        // 최초 실행(빈 DB)에는 과거 메시지로 지도가 도배되지 않도록 저장만 하고 위험 판정은 건너뜀
        boolean firstRun = disasterMessageRepository.count() == 0;
        int page = resolveLatestPage();
        if (page < 1) return 0;

        int saved = 0;
        for (int p = page; p > page - RECENT_PAGES && p >= 1; p--) {
            saved += fetchAndStorePage(p, firstRun);
        }
        return saved;
    }

    // 한 페이지를 조회해 신규 메시지를 저장하고 저장 건수를 반환.
    private int fetchAndStorePage(int pageNo, boolean firstRun) {
        JsonNode items = fetchBody(pageNo);
        if (items == null || !items.isArray() || items.isEmpty()) return 0;

        int saved = 0;
        // 한 페이지 안에서는 오래된 것부터 처리(시간순 저장/로그)
        for (int i = items.size() - 1; i >= 0; i--) {
            JsonNode item = items.get(i);
            long sn = item.path("SN").asLong(0);
            if (sn == 0 || disasterMessageRepository.existsBySn(sn)) continue; // 중복 skip

            String content = item.path("MSG_CN").asText("");
            String area = item.path("RCPTN_RGN_NM").asText("정보 없음");
            String type = item.path("DST_SE_NM").asText("기타");
            // 공식 긴급단계(위급/긴급/안전안내). 보통 EMRG_STEP_NM
            String emergencyLevel = item.path("EMRG_STEP_NM").asText("");
            String sentDate = item.path("CRT_DT").asText("");

            DisasterMessage message = new DisasterMessage(new DisasterMessageDto(type, emergencyLevel, area, sentDate, content));
            message.setSn(sn);
            disasterMessageRepository.save(message);
            saved++;
            log.info("신규 재난문자 저장: {} ({} / {})", area, type, emergencyLevel.isBlank() ? "단계없음" : emergencyLevel);

            if (!firstRun) {
                // 새 메시지를 지도 사이드바 실시간 피드로 push (최초 적재분은 제외)
                messageBroadcaster.broadcast(new DisasterMessageDto(type, emergencyLevel, area, sentDate, content));
                analyzeAndTriggerDisaster(message);
            }
        }
        return saved;
    }

    // 지정 페이지의 body 배열을 반환(없거나 오류면 null). serviceKey는 Encoding 형태라 URI로 넘겨 이중 인코딩 방지.
    private JsonNode fetchBody(int pageNo) {
        try {
            URI uri = new URI(DISASTER_API_URL
                    + "?serviceKey=" + disasterServiceKey
                    + "&returnType=json&pageNo=" + pageNo + "&numOfRows=" + PAGE_SIZE);

            String responseBody = restTemplate.getForObject(uri, String.class);
            if (responseBody == null) return null;

            JsonNode root = objectMapper.readTree(responseBody);
            String resultCode = root.path("header").path("resultCode").asText("");
            if (!resultCode.isEmpty() && !"00".equals(resultCode)) {
                log.warn("긴급재난문자 API 오류: {} - {}", resultCode, root.path("header").path("resultMsg").asText());
                return null;
            }
            return root.path("body");
        } catch (Exception e) {
            log.error("긴급재난문자 API 조회 오류: {}", e.getMessage());
            return null;
        }
    }

    // 마지막(=최신) 페이지를 한 번 찾아 캐시. totalCount가 없어 지수 탐색 → 이분 탐색으로 경계를 찾는다.
    // 이후엔 데이터가 늘어 다음 페이지가 생겼을 때만 한 칸씩 전진.
    private int resolveLatestPage() {
        if (latestPage == 0) {
            int lo = 1, hi = 1;
            while (hasData(hi)) {
                lo = hi;
                if (hi > 1_000_000) break;
                hi *= 2;
            }
            while (lo + 1 < hi) {
                int mid = lo + (hi - lo) / 2;
                if (hasData(mid)) lo = mid; else hi = mid;
            }
            latestPage = lo;
            log.info("긴급재난문자 최신 페이지 탐색: page={}", latestPage);
        } else if (hasData(latestPage + 1)) {
            latestPage++;   // 새 데이터로 다음 페이지가 생김
        }
        return latestPage;
    }

    private boolean hasData(int pageNo) {
        JsonNode body = fetchBody(pageNo);
        return body != null && body.isArray() && !body.isEmpty();
    }

    // 위험 판정은 정부 공식 긴급단계를 그대로 쓴다(권위 출처). 단계가 비어있는 메시지만 AI 보조 추정.
    private void analyzeAndTriggerDisaster(DisasterMessage msg) {
        String level = msg.getEmergencyLevel();
        boolean critical = (level != null && !level.isBlank())
                ? isCriticalLevel(level)                          // 공식 긴급단계 중계
                : aiClientService.isCritical(msg.getContent());   // 단계 없을 때만 AI 보조

        if (critical) {
            disasterService.createAreaDisaster(msg.getArea(), msg.getDisasterType(), 60);
            log.info("위험({}) - 재난 영역 생성: {}", (level == null || level.isBlank()) ? "AI추정" : level, msg.getArea());
        }
    }

    // 위급재난문자·긴급재난문자면 위험. 안전안내문자는 표시만(영역 생성 안 함).
    private boolean isCriticalLevel(String level) {
        return level.contains("위급") || level.contains("긴급");
    }
}
