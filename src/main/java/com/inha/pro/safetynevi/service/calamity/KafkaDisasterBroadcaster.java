package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterEvent;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 운영(HA): 재난 이벤트를 Kafka 토픽에 발행한다.
 * 발행만 하고(fire-and-forget), 실제 클라이언트 전파는 각 인스턴스의 DisasterEventListener가 소비해 처리한다.
 */
@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class KafkaDisasterBroadcaster implements DisasterBroadcaster {

    public static final String TOPIC = "disaster-events";

    private final KafkaTemplate<String, DisasterEvent> kafkaTemplate;

    @Override
    public void broadcastNew(DisasterZoneResponse zone) {
        kafkaTemplate.send(TOPIC, DisasterEvent.ofNew(zone));
    }

    @Override
    public void broadcastDelete(Long id) {
        kafkaTemplate.send(TOPIC, DisasterEvent.ofDelete(id));
    }
}
