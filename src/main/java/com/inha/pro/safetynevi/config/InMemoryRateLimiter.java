package com.inha.pro.safetynevi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비운영(단일 인스턴스): 인메모리 고정 윈도우(1분) 카운터.
 */
@Profile("!prod")
@Component
public class InMemoryRateLimiter implements RateLimiter {

    // 윈도우(1분)당 IP별 허용 요청 수. 정상 사용은 막지 않도록 넉넉히, 설정으로 튜닝 가능
    @Value("${ratelimit.api.per-minute:300}")
    private int maxRequests;

    private static final long WINDOW_MS = 60_000;

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    @Override
    public boolean isOverLimit(String key) {
        long now = System.currentTimeMillis();
        Window w = counters.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.windowStart >= WINDOW_MS) {
                w.windowStart = now;
                w.count = 0;
            }
            w.count++;
            return w.count > maxRequests;
        }
    }

    // 오래된 윈도우 정리(메모리 누수 방지)
    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(e -> now - e.getValue().windowStart > 2 * WINDOW_MS);
    }

    private static class Window {
        volatile long windowStart;
        int count;
        Window(long start) { this.windowStart = start; }
    }
}
