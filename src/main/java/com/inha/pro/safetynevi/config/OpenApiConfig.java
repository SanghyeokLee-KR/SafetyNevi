package com.inha.pro.safetynevi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// /swagger-ui.html 에서 REST API 문서 확인 (springdoc 가 컨트롤러 시그니처로 자동 생성)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI safetyNeviOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("SafetyNevi API")
                .description("재난 대피 플랫폼 REST API (지도·경로·날씨·재난구역·게시판)")
                .version("v0.0.1"));
    }
}
