package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 신고(Report) 처리 API
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final ReportService reportService;

    // 신고 처리 상태 변경 (접수 -> 처리완료)
    @PatchMapping("/reports/{id}/status")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        reportService.updateReportStatus(id, newStatus);
        return ResponseEntity.ok("updated");
    }
}