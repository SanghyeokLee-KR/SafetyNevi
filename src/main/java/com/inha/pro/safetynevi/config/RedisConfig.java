package com.inha.pro.safetynevi.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

// 운영 캐시 = Redis (인스턴스끼리 공유). 직렬화는 기본 JDK 라 캐싱하는 DTO는 Serializable 로 해둠.
// 세션은 spring-session-data-redis 가 알아서 잡아주니 여기 따로 없음.
@Configuration
@Profile("prod")
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // activeDisasters 30초만, 어차피 재난 생기거나 지워지면 evict 됨
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
