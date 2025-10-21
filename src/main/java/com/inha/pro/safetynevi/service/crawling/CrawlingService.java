package com.inha.pro.safetynevi.service.crawling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
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

// 긴급재난문자 공식 API 1분마다 긁어서 신규 저장 + AI 위험판정시 지도에 영역 생성 (전엔 네이버 크롤링이었음)
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final DisasterMessageRepository disasterMessageRepository;
    private final DisasterService disasterService;
    private final AiClientService aiClientService;

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

    // 1분마다 공식 API 조회. 외부 I/O(API·AI) 동안 DB 커넥션을 잡지 않으려 메서드 트랜잭션은 두지 않음
    @Scheduled(fixedDelay = 60000)
    public void fetchDisasterMessages() {
        if (disasterServiceKey == null || disasterServiceKey.isBlank()) return; // 키 미설정 시 동작 안 함

        // 최초 실행(빈 DB)에는 과거 메시지로 지도가 도배되지 않도록 저장만 하고 위험 판정은 건너뜀
        boolean firstRun = disasterMessageRepository.count() == 0;

        try {
            // serviceKey는 인증키(Encoding) 형태 가정. URI 객체로 넘겨 RestTemplate 이중 인코딩을 방지한다.
            // (SERVICE_KEY 오류가 나면 인증키(Decoding) 형태로 바꾸고 UriComponentsBuilder.encode() 사용)
            URI uri = new URI(DISASTER_API_URL
                    + "?serviceKey=" + disasterServiceKey
                    + "&returnType=json&pageNo=1&numOfRows=20");

            String responseBody = restTemplate.getForObject(uri, String.class);
            if (responseBody == null) return;

            JsonNode root = objectMapper.readTree(responseBody);

            String resultCode = root.path("header").path("resultCode").asText("");
            if (!resultCode.isEmpty() && !"00".equals(resultCode)) {
                log.warn("긴급재난문자 API 오류: {} - {}", resultCode, root.path("header").path("resultMsg").asText());
                return;
            }

            JsonNode items = root.path("body");
            if (!items.isArray() || items.isEmpty()) return;

            // API는 최신순 → 오래된 것부터 처리(시간순 저장/로그)
            for (int i = items.size() - 1; i >= 0; i--) {
                JsonNode item = items.get(i);
                long sn = item.path("SN").asLong(0);
                if (sn == 0 || disasterMessageRepository.existsBySn(sn)) continue; // 중복 skip

                String content = item.path("MSG_CN").asText("");
                String area = item.path("RCPTN_RGN_NM").asText("정보 없음");
                String type = item.path("DST_SE_NM").asText("기타");
                String sentDate = item.path("CRT_DT").asText("");

                DisasterMessage message = new DisasterMessage(new DisasterMessageDto(type, area, sentDate, content));
                message.setSn(sn);
                disasterMessageRepository.save(message);
                log.info("신규 재난문자 저장: {} ({})", area, type);

                if (!firstRun) {
                    analyzeAndTriggerDisaster(message);
                }
            }
        } catch (Exception e) {
            log.error("긴급재난문자 API 조회 오류: {}", e.getMessage());
        }
    }

    private void analyzeAndTriggerDisaster(DisasterMessage msg) {
        // AI 서버에 위험도 분석 요청 → 위험하면 지역명 기반으로 지도에 폴리곤(60분) 생성
        if (aiClientService.isCritical(msg.getContent())) {
            disasterService.createAreaDisaster(msg.getArea(), msg.getDisasterType(), 60);
            log.info("위험 판정 - 재난 영역 생성: {}", msg.getArea());
        }
    }
}
