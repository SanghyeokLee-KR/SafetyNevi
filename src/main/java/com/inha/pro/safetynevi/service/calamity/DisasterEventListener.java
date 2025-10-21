package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class DisasterEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // groupId 를 인스턴스마다 다르게(UUID) 해야 모든 인스턴스가 다 받아서 각자 클라이언트한테 뿌린다.
    // 같은 그룹으로 묶으면 한 인스턴스만 받아서 걔한테 붙은 사람만 알림 옴 (주의!)
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
            log.warn("이상한 재난 이벤트 action: {}", event.action());
        }
    }
}
