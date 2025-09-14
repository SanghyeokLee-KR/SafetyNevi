package com.inha.pro.safetynevi.util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 실패 핸들러
 * - 실패 횟수를 기록하고, 잠긴 계정이면 안내 페이지로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        loginAttemptService.loginFailed(username);

        String target = loginAttemptService.isBlocked(username) ? "/login?locked" : "/login?error";
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
