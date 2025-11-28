package com.inha.pro.safetynevi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    // 신뢰 프록시(Nginx) 뒤에서만 XFF 인정. 직접 노출 환경에선 클라가 헤더 위조로 레이트리밋 우회 가능.
    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    // 비로그인 쓰기 엔드포인트(웹푸시 구독/해지)는 일반 조회보다 훨씬 빡빡하게 — 구독 테이블 스팸/남용 방지.
    // NAT·공용 IP 환경을 고려해 너무 낮지 않게. 운영에선 ratelimit.write.per-minute 로 조정.
    @Value("${ratelimit.write.per-minute:30}")
    private int writePerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);

        // 1) 일반 /api 한도(IP당)
        if (rateLimiter.isOverLimit(ip)) {
            tooManyRequests(response);
            return;
        }
        // 2) 비로그인 쓰기 엔드포인트(웹푸시 구독/해지)는 별도 키로 더 빡빡하게
        if (isWriteSensitive(request, uri) && rateLimiter.isOverLimit("w:" + ip, writePerMinute)) {
            log.warn("쓰기 엔드포인트 레이트리밋 초과: {} {}", request.getMethod(), uri);
            tooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    // 스팸이 우려되는 비로그인 쓰기 엔드포인트 (현재: 웹푸시 구독/해지)
    private boolean isWriteSensitive(HttpServletRequest req, String uri) {
        return "POST".equalsIgnoreCase(req.getMethod()) && uri.startsWith("/api/push/");
    }

    private void tooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
    }

    private String clientIp(HttpServletRequest req) {
        // 신뢰 프록시 뒤일 때만 XFF 인정 (아니면 위조로 우회 가능 → 직접 연결 IP 사용)
        if (trustProxy) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
