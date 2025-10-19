package com.inha.pro.safetynevi.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * 운영(prod): Redis 분산 캐시.
 * - 여러 인스턴스가 캐시를 공유한다(@Cacheable("activeDisasters") 가 사용).
 * - 값 직렬화는 기본 JDK 직렬화 — 캐시 대상 DTO(DisasterZoneResponse)는 Serializable.
 * - 세션은 spring-session-data-redis 오토컨피그가 처리하므로 별도 빈이 없다.
 */
@Configuration
@Profile("prod")
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30));   // activeDisasters: 짧게, 생성/삭제 시 @CacheEvict 로 즉시 무효화
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
