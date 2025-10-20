package com.inha.pro.safetynevi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로컬용. 인메모리 고정 윈도우(1분) 카운터
@Profile("!prod")
@Component
public class InMemoryRateLimiter implements RateLimiter {

    // IP별 분당 허용 요청수. 평소 사용은 안 막히게 넉넉히
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

    // 안 쓰는 윈도우 주기적으로 치워줌 (메모리 새는거 방지)
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
