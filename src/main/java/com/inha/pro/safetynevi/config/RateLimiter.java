package com.inha.pro.safetynevi.config;

// 레이트리밋 카운팅. 로컬은 인메모리, 운영은 Redis (인스턴스끼리 공유)
public interface RateLimiter {
    // key(보통 IP)가 분당 한도 넘었으면 true
    boolean isOverLimit(String key);
}
