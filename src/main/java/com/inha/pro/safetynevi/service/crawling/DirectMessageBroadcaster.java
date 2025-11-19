package com.inha.pro.safetynevi.service.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 단일 인스턴스(로컬·기본): 받은 메시지를 곧장 STOMP로 클라이언트에 전달
@Profile("!prod")
@Component
@RequiredArgsConstructor
public class DirectMessageBroadcaster implements MessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(DisasterMessageDto message) {
        messagingTemplate.convertAndSend("/topic/disaster-message", message);
    }
}
