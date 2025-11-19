package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.service.calamity.KafkaDisasterBroadcaster;
import com.inha.pro.safetynevi.service.crawling.KafkaMessageBroadcaster;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

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

    @Bean
    public NewTopic disasterMessagesTopic() {
        return TopicBuilder.name(KafkaMessageBroadcaster.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
