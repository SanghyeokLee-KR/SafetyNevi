package com.inha.pro.safetynevi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * - 로컬 파일 업로드 경로를 리소스 핸들러에 매핑
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 외부 경로의 파일들을 URL로 접근 가능하도록 매핑
        registry.addResourceHandler("/images/uploads/**")
                .addResourceLocations("file:///" + uploadDir + "/");

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:///" + uploadDir);
    }
}