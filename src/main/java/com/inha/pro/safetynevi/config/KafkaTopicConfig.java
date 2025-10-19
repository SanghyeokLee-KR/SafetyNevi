package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.service.calamity.KafkaDisasterBroadcaster;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 운영(prod): 재난 이벤트 토픽을 앱 기동 시 자동 생성(KafkaAdmin).
 */
@Configuration
@Profile("prod")
public class KafkaTopicConfig {

    @Bean
    public NewTopic disasterEventsTopic() {
        return TopicBuilder.name(KafkaDisasterBroadcaster.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
