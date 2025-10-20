package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 로컬/단일 인스턴스용. 카프카 없이 웹소켓으로 바로 쏜다
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
