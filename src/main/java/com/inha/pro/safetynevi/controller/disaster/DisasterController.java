package com.inha.pro.safetynevi.controller.disaster;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 실시간 재난 현황 조회 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DisasterController {

    private final DisasterService disasterService;

    @GetMapping("/api/disaster-zones")
    public List<DisasterZoneResponse> getActiveDisasterZones() {
        // 캐시된 DTO 목록을 그대로 반환 (운영은 Redis, 로컬은 Caffeine)
        return disasterService.getActiveDisasterZones();
    }
}