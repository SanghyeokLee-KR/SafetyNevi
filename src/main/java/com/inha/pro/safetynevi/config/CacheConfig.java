package com.inha.pro.safetynevi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

// 로컬 캐시 = Caffeine. 운영은 RedisConfig 가 대신함
// (날씨 캐시는 reactive 라 WeatherService 안에서 따로 Caffeine 씀)
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!prod")
    public CacheManager cacheManager() {
        // activeDisasters: 지도 뜰 때마다 부르는거라 30초 캐싱
        CaffeineCacheManager manager = new CaffeineCacheManager("activeDisasters");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(100));
        return manager;
    }
}
