package com.inha.pro.safetynevi.config;

public interface RateLimiter {
    /** 기본 한도(ratelimit.api.per-minute) 기준 초과 여부. */
    boolean isOverLimit(String key);

    /** 지정한 분당 한도 기준 초과 여부, 엔드포인트별 더 빡빡한 제한용. */
    boolean isOverLimit(String key, int maxPerMinute);
}
