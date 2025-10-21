package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminRestController {

    private final DashboardService dashboardService;

    @PostMapping("/dashboardChart")
    public Map<String, Object> dashboardChart() {
        return dashboardService.dashboardChart();
    }
}