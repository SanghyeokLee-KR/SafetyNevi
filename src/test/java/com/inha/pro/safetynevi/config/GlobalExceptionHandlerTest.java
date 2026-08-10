package com.inha.pro.safetynevi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 클라이언트 잘못이 500 으로 뭉개지지 않는지 고정한다.
 * catch-all(Exception) 핸들러가 Spring MVC 의 4xx 예외를 먼저 가져가면
 * 호출자가 고칠 수 있는 오류가 서버 장애로 보고된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 필수_파라미터_누락은_400() throws Exception {
        // /api/facilities 는 type 외에 bbox 4개(swLat, swLng, neLat, neLng)가 필수다
        mvc.perform(get("/api/facilities").param("type", "shelter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void 파라미터_타입_불일치는_400() throws Exception {
        mvc.perform(get("/api/facilities")
                        .param("type", "shelter")
                        .param("swLat", "숫자아님")
                        .param("swLng", "126.8")
                        .param("neLat", "37.7")
                        .param("neLng", "127.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void 안전점수도_좌표_없으면_400() throws Exception {
        mvc.perform(get("/api/safety-score"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 정상_요청은_200() throws Exception {
        // 400 을 넓게 잡다가 정상 경로까지 막는 회귀를 방지한다
        mvc.perform(get("/api/facilities")
                        .param("type", "shelter")
                        .param("swLat", "37.4")
                        .param("swLng", "126.8")
                        .param("neLat", "37.7")
                        .param("neLng", "127.2"))
                .andExpect(status().isOk());
    }
}
