package com.inha.pro.safetynevi.service.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// 운영(다중 인스턴스): Kafka로 발행만 한다. 받아서 각 인스턴스가 클라이언트로 뿌리는 건 MessageKafkaListener.
// (재난 zone fan-out과 같은 구조, 락 잡은 한 인스턴스만 수집해도 모든 인스턴스의 접속자가 실시간으로 받게)
@Profile("prod")
@Component
@RequiredArgsConstructor
public class KafkaMessageBroadcaster implements MessageBroadcaster {

    public static final String TOPIC = "disaster-messages";

    private final KafkaTemplate<String, DisasterMessageDto> kafkaTemplate;

    @Override
    public void broadcast(DisasterMessageDto message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
