package com.inha.pro.safetynevi.controller.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.inha.pro.safetynevi.dto.map.RouteDto;
import com.inha.pro.safetynevi.service.map.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 경로 탐색 및 추천 API
 */
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // 최적 대피소 추천 목록 조회
    @GetMapping("/recommend")
    public ResponseEntity<List<RouteDto>> getRecommendedRoutes(@RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(routeService.getOptimalShelters(lat, lon));
    }

    // 경로 상세 탐색 (Kakao Mobility API 연동)
    @GetMapping("/path")
    public ResponseEntity<?> getRoutePath(
            @RequestParam double startLat, @RequestParam double startLon,
            @RequestParam double endLat, @RequestParam double endLon
    ) {
        JsonNode routeData = routeService.getKakaoRoute(startLat, startLon, endLat, endLon);
        if (routeData == null) {
            return ResponseEntity.status(500).body("Path finding failed");
        }
        return ResponseEntity.ok(routeData);
    }
}