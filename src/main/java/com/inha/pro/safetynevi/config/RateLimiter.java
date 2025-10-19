package com.inha.pro.safetynevi.config;

/**
 * 레이트리밋 카운팅 전략.
 * - 비운영(단일 인스턴스): 인메모리 (InMemoryRateLimiter)
 * - 운영(HA): Redis 로 인스턴스 간 공유 (RedisRateLimiter)
 */
public interface RateLimiter {
    /** key(보통 클라이언트 IP)가 분당 허용량을 초과했으면 true. */
    boolean isOverLimit(String key);
}
