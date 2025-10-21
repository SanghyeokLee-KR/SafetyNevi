package com.inha.pro.safetynevi.service.ai;

import com.inha.pro.safetynevi.dto.ai.AiRequestDto;
import com.inha.pro.safetynevi.dto.ai.AiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientService {

    private final RestTemplate restTemplate = createRestTemplate();
    private static final String AI_URL = "http://localhost:8000/predict";

    // AI 서버가 응답하지 않을 때 호출 스레드(크롤러 등)가 무한 대기하지 않도록 타임아웃을 둔다.
    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 연결 3초
        factory.setReadTimeout(5000);    // 응답 5초
        return new RestTemplate(factory);
    }

    public AiResponseDto analyze(String text) {
        AiRequestDto req = new AiRequestDto(text);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<AiResponseDto> response =
                    restTemplate.postForEntity(AI_URL, new HttpEntity<>(req, headers), AiResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("AI Server Error: {}", e.getMessage());
            // AI 서버 죽어도 크롤링은 계속 돌아야 하니 일단 SAFE로 처리
            return new AiResponseDto("UNKNOWN", "SAFE", 0.0);
        }
    }

    public boolean isCritical(String text) {
        AiResponseDto result = analyze(text);
        return "DANGER".equalsIgnoreCase(result.getSafety());
    }
}