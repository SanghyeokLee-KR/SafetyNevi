package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 비운영(단일 인스턴스): 재난 이벤트를 WebSocket으로 바로 전파.
 * Kafka 없이 로컬에서 그대로 동작한다.
 */
@Profile("!prod")
@Component
@RequiredArgsConstructor
public class DirectDisasterBroadcaster implements DisasterBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastNew(DisasterZoneResponse zone) {
        messagingTemplate.convertAndSend("/topic/disaster/new", zone);
    }

    @Override
    public void broadcastDelete(Long id) {
        messagingTemplate.convertAndSend("/topic/disaster/delete", id);
    }
}
