package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterEvent;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// 발행만 한다. 받아서 클라이언트로 뿌리는건 DisasterEventListener
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
