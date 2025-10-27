package com.inha.pro.safetynevi.util;

import com.inha.pro.safetynevi.dao.member.AccessLogRepository;
import com.inha.pro.safetynevi.entity.member.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그아웃 시 접속로그 한 줄 남기고 메인으로
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AccessLogRepository accessLogRepository;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        if (authentication != null) {
            String userId = authentication.getName();
            String ip = ClientUtils.getRemoteIP(request, trustProxy);
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