package com.inha.pro.safetynevi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 운영(HA): Redis 고정 윈도우(1분) 카운터 — 여러 인스턴스가 카운트를 공유한다.
 * 분 단위 버킷 키에 INCR, 첫 증가 때만 TTL 1분 설정 → 만료는 Redis가 알아서(정리 불필요).
 */
@Profile("prod")
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    @Value("${ratelimit.api.per-minute:300}")
    private int maxRequests;

    private final StringRedisTemplate redis;

    @Override
    public boolean isOverLimit(String key) {
        long minute = System.currentTimeMillis() / 60_000;
        String redisKey = "ratelimit:" + key + ":" + minute;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofMinutes(1));
        }
        return count != null && count > maxRequests;
    }
}
