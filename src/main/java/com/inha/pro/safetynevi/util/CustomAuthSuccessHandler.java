package com.inha.pro.safetynevi.util;

import com.inha.pro.safetynevi.dao.member.AccessLogRepository;
import com.inha.pro.safetynevi.entity.member.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 성공하면 접속로그(IP/브라우저/ID) 남기고 메인으로
@Component
@RequiredArgsConstructor
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AccessLogRepository accessLogRepository;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String userId = authentication.getName();
        loginAttemptService.loginSucceeded(userId);

        String ip = ClientUtils.getRemoteIP(request, trustProxy);
        String simpleUA = ClientUtils.getBrowserInfo(request.getHeader("User-Agent"));

        AccessLog log = AccessLog.builder()
                .userId(userId)
                .accessType("LOGIN")
                .ipAddress(ip)
                .userAgent(simpleUA)
                .build();

        accessLogRepository.save(log);
        response.sendRedirect("/");
    }
}