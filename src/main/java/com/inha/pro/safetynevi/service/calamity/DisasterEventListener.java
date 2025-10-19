package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 운영(HA): Kafka 재난 이벤트를 소비해, 이 인스턴스에 연결된 WebSocket 클라이언트로 전파한다.
 * groupId 를 인스턴스마다 고유(UUID)하게 둬서 모든 인스턴스가 모든 이벤트를 받게 한다(fan-out).
 * (공유 그룹이면 한 인스턴스만 소비 → 그 인스턴스 클라이언트만 알림 받는 문제가 생긴다)
 */
@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class DisasterEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = KafkaDisasterBroadcaster.TOPIC,
            groupId = "#{'safetynevi-' + T(java.util.UUID).randomUUID()}"
    )
    public void onDisasterEvent(DisasterEvent event) {
        if ("NEW".equals(event.action())) {
            messagingTemplate.convertAndSend("/topic/disaster/new", event.toZone());
        } else if ("DELETE".equals(event.action())) {
            messagingTemplate.convertAndSend("/topic/disaster/delete", event.id());
        } else {
            log.warn("알 수 없는 재난 이벤트 action: {}", event.action());
        }
    }
}
