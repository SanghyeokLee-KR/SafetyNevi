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
                // 보안 응답 헤더 (nosniff·X-Frame-Options·HSTS 는 Spring 기본 유지 + Referrer-Policy·CSP 추가)
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // CSP: 외부 리소스 허용목록 (카카오 지도/주소, SockJS, Pretendard, 유튜브 등).
                        // 호스트에 http/https 안 붙여서 로컬이랑 운영 둘 다 되게
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' dapi.kakao.com *.daumcdn.net cdnjs.cloudflare.com cdn.jsdelivr.net code.jquery.com; " +
                                "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
                                "font-src 'self' data: cdn.jsdelivr.net; " +
                                "img-src 'self' data: blob: http: https:; " +
                                "connect-src 'self' dapi.kakao.com *.daumcdn.net *.kakao.com; " +
                                "frame-src 'self' www.youtube.com *.daumcdn.net *.daum.net *.kakao.com; " +
                                "object-src 'none'; base-uri 'self'; form-action 'self'"
                        ))
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
                        .requestMatchers("/api/disaster-messages/**").permitAll()
                        .requestMatchers("/api/board").permitAll()

                        // 재난 웹푸시 구독 (비로그인도 허용) + 백그라운드 수신용 서비스워커
                        .requestMatchers("/api/push/**").permitAll()
                        .requestMatchers("/push-sw.js", "/sw-register.js", "/manifest.webmanifest").permitAll()

                        // 관리자 전용
                        .requestMatchers("/admin/**", "/api/admin/**", "/dashboardChart").hasRole("ADMIN")

                        // Actuator: health는 공개(로드밸런서·업타임 체크), 나머지(prometheus/info)는 관리자
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // API 문서 (Swagger UI / OpenAPI). 운영 배포 시엔 ADMIN 제한 고려
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

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