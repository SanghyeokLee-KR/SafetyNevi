package com.inha.pro.safetynevi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        if (rateLimiter.isOverLimit(clientIp(request))) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest req) {
        // Nginx 등 리버스 프록시 뒤에서는 실제 클라이언트 IP가 X-Forwarded-For 에 담긴다
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
