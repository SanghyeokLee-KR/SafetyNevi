package com.inha.pro.safetynevi.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 접근 거부(403 Forbidden) 핸들러
 * - 권한이 없는 페이지 접근 시 메인 페이지로 리다이렉트 (에러 파라미터 포함)
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        // 보안상 자세한 에러 메시지 노출을 피하고 리다이렉트 처리
        response.sendRedirect("/?error=denied");
    }
}