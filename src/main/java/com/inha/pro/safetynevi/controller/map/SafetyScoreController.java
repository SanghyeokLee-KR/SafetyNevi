package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.SafetyScoreResponse;
import com.inha.pro.safetynevi.service.map.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 한 지점(내 위치 또는 클릭 지점)의 대피 접근성 점수. 지도 화면 범위와 무관하게 동작.
@RestController
@RequiredArgsConstructor
public class SafetyScoreController {

    private final SafetyScoreService safetyScoreService;

    @GetMapping("/api/safety-score")
    public SafetyScoreResponse score(@RequestParam double lat, @RequestParam double lng) {
        return safetyScoreService.evaluate(lat, lng);
    }
}
