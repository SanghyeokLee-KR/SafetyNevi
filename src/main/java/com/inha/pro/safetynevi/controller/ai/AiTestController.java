package com.inha.pro.safetynevi.controller.ai;

import com.inha.pro.safetynevi.service.ai.AiClientService;
import com.inha.pro.safetynevi.dto.ai.AiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 분석 테스트 컨트롤러 (개발 및 디버깅용)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-test")
public class AiTestController {

    private final AiClientService aiClientService;

    // 텍스트 재난 분석 요청 테스트
    @GetMapping("/analyze")
    public String analyze(@RequestParam(required = false) String text) {
        if (text == null || text.trim().isEmpty()) {
            text = "(No Input)";
        }

        AiResponseDto result = aiClientService.analyze(text);
        String color = "DANGER".equals(result.getSafety()) ? "red" : "blue";

        return String.format(
                "<html><body><h2>AI Analysis Result</h2>" +
                        "<ul><li>Type: %s</li><li>Safety: <b style='color:%s'>%s</b></li><li>Confidence: %.2f%%</li><li>Input: %s</li></ul>" +
                        "</body></html>",
                result.getDisasterType(), color, result.getSafety(), result.getConfidence() * 100, text
        );
    }
}