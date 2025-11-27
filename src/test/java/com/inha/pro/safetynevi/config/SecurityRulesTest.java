package com.inha.pro.safetynevi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig 접근 규칙 검증 (특히 actuator 공개/관리자 규칙)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheusRequiresAuth() throws Exception {
        // 비로그인 → 로그인 페이지로 리다이렉트(관리자 전용)
        mvc.perform(get("/actuator/prometheus")).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminPageRequiresAuth() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void apiDocsArePublic() throws Exception {
        // springdoc OpenAPI 문서가 인증 없이 200 (Swagger 동작 + permitAll 규칙 검증)
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void disasterMessagesRecentIsPublic() throws Exception {
        // 지도 사이드바 실시간 피드용 — 비로그인도 최근 재난문자를 받는다(공개 데이터)
        mvc.perform(get("/api/disaster-messages/recent")).andExpect(status().isOk());
    }

    @Test
    void pushConfigIsPublic() throws Exception {
        // 웹푸시 구독에 필요한 VAPID 공개키 조회 — 비로그인도 허용
        mvc.perform(get("/api/push/config")).andExpect(status().isOk());
    }

    @Test
    void adminPushTestRequiresAuth() throws Exception {
        // 관리자 테스트 발송(/api/admin/**) — 비로그인은 로그인 페이지로 리다이렉트
        mvc.perform(get("/api/admin/push/test")).andExpect(status().is3xxRedirection());
    }
}
