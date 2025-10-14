package com.inha.pro.safetynevi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공개 API 남용 방지용 IP 단위 레이트리밋 (고정 윈도우 카운터).
 * 단일 인스턴스 인메모리 방식 — 다중 인스턴스(HA)에서는 Redis 기반으로 교체 필요.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 윈도우(1분)당 IP별 허용 요청 수. 정상 사용은 막지 않도록 넉넉히, 설정으로 튜닝 가능
    @Value("${ratelimit.api.per-minute:300}")
    private int maxRequests;

    private static final long WINDOW_MS = 60_000;

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        if (isOverLimit(clientIp(request))) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isOverLimit(String ip) {
        long now = System.currentTimeMillis();
        Window w = counters.computeIfAbsent(ip, k -> new Window(now));
        synchronized (w) {
            if (now - w.windowStart >= WINDOW_MS) {
                w.windowStart = now;
                w.count = 0;
            }
            w.count++;
            return w.count > maxRequests;
        }
    }

    private String clientIp(HttpServletRequest req) {
        // Nginx 등 리버스 프록시 뒤에서는 실제 클라이언트 IP가 X-Forwarded-For 에 담긴다
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
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
