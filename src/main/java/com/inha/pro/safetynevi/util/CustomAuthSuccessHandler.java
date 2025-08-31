package com.inha.pro.safetynevi.util;

import com.inha.pro.safetynevi.dao.member.AccessLogRepository;
import com.inha.pro.safetynevi.entity.member.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 성공 핸들러
 * - 접속 로그(IP, Browser, ID) 저장 후 메인 페이지로 리다이렉트
 */
@Component
@RequiredArgsConstructor
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AccessLogRepository accessLogRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String userId = authentication.getName();
        String ip = ClientUtils.getRemoteIP(request);
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