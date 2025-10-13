package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.service.member.CustomUserDetailsService;
import com.inha.pro.safetynevi.util.CustomAccessDeniedHandler;
import com.inha.pro.safetynevi.util.CustomAuthFailureHandler;
import com.inha.pro.safetynevi.util.CustomAuthSuccessHandler;
import com.inha.pro.safetynevi.util.CustomLogoutSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthSuccessHandler customAuthSuccessHandler;
    private final CustomAuthFailureHandler customAuthFailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 보호 활성화 (WebSocket 핸드셰이크/SockJS 폴백은 제외)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))
                // 보안 응답 헤더 (nosniff·X-Frame-Options·HSTS 는 Spring 기본 유지 + Referrer-Policy 추가)
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", "/upload/**", "/images/**").permitAll()

                        // 공개 페이지 (메인, 로그인, 지도, 공지사항 등)
                        .requestMatchers("/", "/signup", "/login", "/findAccount").permitAll()
                        .requestMatchers("/map", "/disasterMessage", "/shelterDetail").permitAll()
                        .requestMatchers("/notice", "/noticeDetail", "/disasterGuide").permitAll()

                        // 공개 API (시설 조회, 경로 탐색, 날씨, 게시글 목록)
                        .requestMatchers("/api/check/**", "/api/find/**").permitAll()
                        .requestMatchers("/api/facilities/**", "/api/route/**", "/api/weather/**", "/api/disaster-zones/**").permitAll()
                        .requestMatchers("/api/board").permitAll()

                        // 관리자 전용
                        .requestMatchers("/admin/**", "/api/admin/**", "/dashboardChart").hasRole("ADMIN")

                        // Actuator: health는 공개(로드밸런서·업타임 체크), 나머지(prometheus/info)는 관리자
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // 그 외 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .userDetailsService(customUserDetailsService)

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthSuccessHandler)
                        .failureHandler(customAuthFailureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}