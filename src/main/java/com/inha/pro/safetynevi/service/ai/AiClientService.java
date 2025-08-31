package com.inha.pro.safetynevi.service.ai;

import com.inha.pro.safetynevi.dto.ai.AiRequestDto;
import com.inha.pro.safetynevi.dto.ai.AiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

/**
 * AI 분석 서버 연동 서비스
 * - RestTemplate을 사용하여 외부 AI API에 텍스트 분석 요청
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String AI_URL = "http://localhost:8000/predict";

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
            // 장애 시 기본값 반환 (Fail-safe)
            return new AiResponseDto("UNKNOWN", "SAFE", 0.0);
        }
    }

    public boolean isCritical(String text) {
        AiResponseDto result = analyze(text);
        return "DANGER".equalsIgnoreCase(result.getSafety());
    }
}