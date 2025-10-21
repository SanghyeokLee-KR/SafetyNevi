package com.inha.pro.safetynevi.config;

public interface RateLimiter {
    boolean isOverLimit(String key);
}
