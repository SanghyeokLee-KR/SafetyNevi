package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.service.dashboard.DashboardService;
import com.inha.pro.safetynevi.service.push.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminRestController {

    private final DashboardService dashboardService;
    private final WebPushService webPushService;

    @PostMapping("/dashboardChart")
    public Map<String, Object> dashboardChart() {
        return dashboardService.dashboardChart();
    }

    // 관리자 테스트 푸시 발송 (/api/admin/** → ADMIN 전용). 구독 기기로 테스트 알림을 보내고 성공 건수를 돌려준다.
    @PostMapping("/api/admin/push/test")
    public Map<String, Object> sendTestPush() {
        int sent = webPushService.sendTestNotification();
        return Map.of("enabled", webPushService.isEnabled(), "sent", sent);
    }
}