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
    public void subscribe(String endpoint, String p256dh, String auth, String userId, String userAgent) {
        if (endpoint == null || endpoint.isBlank()) return;
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setUserId(userId);
        sub.setUserAgent(userAgent);
        subscriptionRepository.save(sub);
        log.info("웹푸시 구독 등록: user={}", userId == null ? "익명" : userId);
    }

    // 구독 해지
    @Transactional
    public void unsubscribe(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        subscriptionRepository.findByEndpoint(endpoint).ifPresent(subscriptionRepository::delete);
    }

    // 새 재난 발생 시 등록된 모든 구독으로 푸시. (지역 구독은 다음 단계 — 지금은 전체 발송)
    // 외부 푸시 서비스로의 HTTP 호출이라 트랜잭션 밖에서 — DB 커넥션을 쥔 채 네트워크를 기다리지 않도록.
    public void notifyNewDisaster(DisasterZoneResponse zone) {
        PushService service = push();
        if (service == null) return;   // 미설정 → no-op

        List<PushSubscription> subs = subscriptionRepository.findAll();
        if (subs.isEmpty()) return;

        byte[] payload = buildPayload(zone);
        List<PushSubscription> dead = new ArrayList<>();
        int success = 0;

        // Web Push 는 멀티캐스트가 없어 구독마다 한 번씩 보낸다. 만료(404/410)된 구독은 정리.
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
        log.info("웹푸시 재난 발송: 대상 {}건, 성공 {}건", subs.size(), success);
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
}
