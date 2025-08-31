package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.service.member.CustomUserDetailsService;
import com.inha.pro.safetynevi.util.CustomAccessDeniedHandler;
import com.inha.pro.safetynevi.util.CustomAuthSuccessHandler;
import com.inha.pro.safetynevi.util.CustomLogoutSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthSuccessHandler customAuthSuccessHandler;
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
                .csrf(AbstractHttpConfigurer::disable)
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
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

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
                        .failureUrl("/login?error")
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