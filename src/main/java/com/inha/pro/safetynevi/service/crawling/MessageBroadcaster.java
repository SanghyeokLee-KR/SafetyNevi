package com.inha.pro.safetynevi.service.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;

// 새 재난문자를 지도 사이드바 실시간 피드(/topic/disaster-message)로 보내는 통로.
// 단일 인스턴스는 바로 STOMP, 운영(다중)은 Kafka 경유로 모든 인스턴스가 fan-out 한다.
public interface MessageBroadcaster {
    void broadcast(DisasterMessageDto message);
}
