package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 관리자 대시보드 통계 데이터 API
 */
@RestController
@RequiredArgsConstructor
public class AdminRestController {

    private final DashboardService dashboardService;

    // 대시보드 차트용 통계 데이터 반환
    @PostMapping("/dashboardChart")
    public Map<String, Object> dashboardChart() {
        return dashboardService.dashboardChart();
    }
}