package com.inha.pro.safetynevi.service.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// Kafka에서 새 재난문자를 받아 이 인스턴스에 붙은 클라이언트의 피드로 전달.
@Profile("prod")
@Component
@RequiredArgsConstructor
public class MessageKafkaListener {

    private final SimpMessagingTemplate messagingTemplate;

    // groupId를 인스턴스마다 UUID로 다르게 → 모든 인스턴스가 받아 각자 fan-out (공유 그룹이면 한 곳만 받음).
    // 공유 소비자는 DisasterEvent로 역직렬화하므로, 이 리스너만 default.type을 DisasterMessageDto로 오버라이드.
    @KafkaListener(
            topics = KafkaMessageBroadcaster.TOPIC,
            groupId = "#{'safetynevi-msg-' + T(java.util.UUID).randomUUID()}",
            properties = {
                    "spring.json.value.default.type=com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto",
                    "spring.json.trusted.packages=com.inha.pro.safetynevi.dto.crawling"
            }
    )
    public void onMessage(DisasterMessageDto message) {
        messagingTemplate.convertAndSend("/topic/disaster-message", message);
    }
}
