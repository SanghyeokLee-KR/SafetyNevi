package com.inha.pro.safetynevi.service.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inha.pro.safetynevi.dao.push.PushSubscriptionRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.entity.push.PushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository subscriptionRepository;

    // PushService 빈은 VAPID 키가 있을 때만 존재한다(WebPushConfig).
    // 키가 없으면 null → 발송 비활성(앱은 정상 부팅). 재난문자 키 없을 때처럼 조용히 no-op 한다.
    private final ObjectProvider<PushService> pushServiceProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PushService push() {
        return pushServiceProvider.getIfAvailable();
    }

    public boolean isEnabled() {
        return push() != null;
    }

    // 구독 저장. 같은 endpoint 가 다시 오면 갱신(upsert).
    @Transactional
    public void subscribe(String endpoint, String p256dh, String auth, String region, String userId, String userAgent) {
        if (endpoint == null || endpoint.isBlank()) return;
        // endpoint 는 클라이언트가 보내고, 재난 때 서버가 그 주소로 직접 발송한다.
        // 내부망/로컬로 향하는 SSRF 를 막기 위해 공인 https 주소만 받는다.
        if (!isSafePushEndpoint(endpoint)) {
            log.warn("웹푸시 구독 거부, 허용되지 않는 endpoint 형식");
            return;
        }
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setUserId(userId);
        sub.setUserAgent(truncate(userAgent, 300));
        // 관심 지역(시/도). 모르는 값은 전국(null)으로 정규화. 재동기화처럼 region 이 안 오면 기존 선택 유지.
        if (region != null && !region.isBlank()) sub.setAreaName(normalizeRegion(region));
        subscriptionRepository.save(sub);
        log.info("웹푸시 구독 등록: user={}, region={}", userId == null ? "익명" : userId, sub.getAreaName());
    }

    // 구독 해지
    @Transactional
    public void unsubscribe(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        subscriptionRepository.findByEndpoint(endpoint).ifPresent(subscriptionRepository::delete);
    }

    // 새 재난 발생 시, 재난 지역과 같은 시/도를 구독한(또는 전국 구독) 기기로만 푸시.
    // 외부 푸시 서비스로의 HTTP 호출이라 트랜잭션 밖에서, DB 커넥션을 쥔 채 네트워크를 기다리지 않도록.
    public void notifyNewDisaster(DisasterZoneResponse zone) {
        PushService service = push();
        if (service == null) return;   // 미설정 → no-op

        List<PushSubscription> subs = filterByRegion(subscriptionRepository.findAll(), zone.getAreaName());
        if (subs.isEmpty()) return;

        int success = sendAll(service, subs, buildPayload(zone));
        log.info("웹푸시 재난 발송: 대상 {}건, 성공 {}건", subs.size(), success);
    }

    // 관리자 테스트 발송, 구독한 모든 기기로 '테스트' 알림을 보내 발송 경로를 점검한다. 성공 건수 반환.
    public int sendTestNotification() {
        PushService service = push();
        if (service == null) return 0;   // 미설정 → no-op

        List<PushSubscription> subs = subscriptionRepository.findAll();
        if (subs.isEmpty()) return 0;

        int success = sendAll(service, subs, testPayload());
        log.info("웹푸시 테스트 발송: 대상 {}건, 성공 {}건", subs.size(), success);
        return success;
    }

    // 구독마다 한 번씩 발송(Web Push 는 멀티캐스트가 없다). 만료(404/410)된 구독은 정리. 성공 건수 반환.
    private int sendAll(PushService service, List<PushSubscription> subs, byte[] payload) {
        List<PushSubscription> dead = new ArrayList<>();
        int success = 0;
        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
                HttpResponse response = service.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200 || status == 201) {
                    success++;
                } else if (status == 404 || status == 410) {
                    dead.add(sub);
                } else {
                    log.warn("웹푸시 발송 실패 status={}", status);
                }
            } catch (Exception e) {
                log.warn("웹푸시 발송 오류: {}", e.getMessage());
            }
        }
        if (!dead.isEmpty()) {
            subscriptionRepository.deleteAll(dead);
            log.info("웹푸시 만료 구독 정리: {}건", dead.size());
        }
        return success;
    }

    // 재난 지역의 시/도를 모르면(원형 재난 등) 전체에, 알면 같은 시/도·전국 구독자에게만. (테스트용 package-private)
    static List<PushSubscription> filterByRegion(List<PushSubscription> subs, String disasterArea) {
        String province = provinceOf(disasterArea);
        if (province == null) return subs;
        List<PushSubscription> matched = new ArrayList<>();
        for (PushSubscription s : subs) {
            String region = s.getAreaName();
            if (region == null || region.isBlank() || "전국".equals(region) || province.equals(provinceOf(region))) {
                matched.add(s);
            }
        }
        return matched;
    }

    // 주소/지역명에서 시/도를 짧은 형태로 추출. 못 찾으면 null. (지역 구독 매칭·회원 기본지역에서 공용)
    public static String provinceOf(String area) {
        if (area == null || area.isBlank()) return null;
        if (area.contains("서울")) return "서울";
        if (area.contains("부산")) return "부산";
        if (area.contains("대구")) return "대구";
        if (area.contains("인천")) return "인천";
        if (area.contains("광주")) return "광주";
        if (area.contains("대전")) return "대전";
        if (area.contains("울산")) return "울산";
        if (area.contains("세종")) return "세종";
        if (area.contains("경기")) return "경기";
        if (area.contains("강원")) return "강원";
        if (area.contains("충청북") || area.contains("충북")) return "충북";
        if (area.contains("충청남") || area.contains("충남")) return "충남";
        if (area.contains("전라북") || area.contains("전북")) return "전북";
        if (area.contains("전라남") || area.contains("전남")) return "전남";
        if (area.contains("경상북") || area.contains("경북")) return "경북";
        if (area.contains("경상남") || area.contains("경남")) return "경남";
        if (area.contains("제주")) return "제주";
        return null;
    }

    // 관심 지역을 알려진 시/도 또는 "전국"으로만 정규화. 그 외/모르는 값은 null(=전국, 모든 지역)로 본다.
    static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) return null;
        if ("전국".equals(region.trim())) return "전국";
        return provinceOf(region);
    }

    // 구독 endpoint 안전성, https + 공인 도메인만 허용. IP 리터럴/로컬호스트는 내부망 SSRF 우려로 거부.
    // (완전한 SSRF 방어는 네트워크 egress 차단이 정석. 앱 단에선 명백한 내부 주소를 막는다.)
    static boolean isSafePushEndpoint(String endpoint) {
        if (endpoint == null) return false;
        final URI uri;
        try {
            uri = URI.create(endpoint.trim());
        } catch (Exception e) {
            return false;
        }
        if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        if (host == null || host.isBlank()) return false;
        String h = host.toLowerCase();
        if (h.equals("localhost") || h.endsWith(".localhost")) return false;
        return !isIpLiteral(h);
    }

    private static boolean isIpLiteral(String host) {
        if (host.indexOf(':') >= 0) return true;                 // IPv6 리터럴
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}");          // IPv4 점10진
    }

    // 컬럼 길이를 넘는 입력은 잘라 저장(저장 실패 방지).
    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private byte[] buildPayload(DisasterZoneResponse zone) {
        String type = (zone.getDisasterType() == null || zone.getDisasterType().isBlank()) ? "재난" : zone.getDisasterType();
        String area = zone.getAreaName() != null ? zone.getAreaName()
                : (zone.getLatitude() != null ? "현재 위치 인근" : "");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("title", "[긴급재난] " + type);
        data.put("body", (area.isBlank() ? "" : area + " ") + "재난이 발생했습니다. 안전에 유의하세요.");
        data.put("url", "/disasterMessage");
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            return "{\"title\":\"안전네비 재난 알림\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] testPayload() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("title", "[테스트] 안전네비 알림");
        data.put("body", "재난 알림이 정상 동작합니다. (관리자 테스트 발송)");
        data.put("url", "/disasterMessage");
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            return "{\"title\":\"안전네비 테스트\"}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
