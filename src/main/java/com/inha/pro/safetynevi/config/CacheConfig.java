package com.inha.pro.safetynevi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 캐시 설정 (Caffeine 인메모리, 단일 인스턴스).
 * - activeDisasters: 활성 재난구역 목록. 지도 로드마다 조회되므로 캐시한다.
 *   재난 생성/삭제 때 무효화(@CacheEvict)하고, 시간 만료는 30초 TTL 로 흡수한다.
 * (날씨 KMA 캐시는 reactive 흐름이라 WeatherService 안에서 Caffeine 을 직접 쓴다)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("activeDisasters");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(100));
        return manager;
    }
}
