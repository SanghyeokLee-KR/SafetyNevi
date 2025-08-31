package com.inha.pro.safetynevi.util;

import com.inha.pro.safetynevi.dao.member.AccessLogRepository;
import com.inha.pro.safetynevi.entity.member.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그아웃 성공 핸들러
 * - 로그아웃 로그 저장 후 메인 페이지로 리다이렉트
 */
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AccessLogRepository accessLogRepository;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        if (authentication != null) {
            String userId = authentication.getName();
            String ip = ClientUtils.getRemoteIP(request);
            String simpleUA = ClientUtils.getBrowserInfo(request.getHeader("User-Agent"));

            AccessLog log = AccessLog.builder()
                    .userId(userId)
                    .accessType("LOGOUT")
                    .ipAddress(ip)
                    .userAgent(simpleUA)
                    .build();
            accessLogRepository.save(log);
        }
        response.sendRedirect("/");
    }
}