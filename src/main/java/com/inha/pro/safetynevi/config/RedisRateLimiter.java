package com.inha.pro.safetynevi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// 운영용. Redis 고정 윈도우 카운터라 인스턴스들이 카운트를 같이 본다.
// 분 단위 키에 INCR 하고 처음 만들 때만 TTL 1분 — 만료는 Redis가 알아서 하니 따로 안 치워도 됨
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
