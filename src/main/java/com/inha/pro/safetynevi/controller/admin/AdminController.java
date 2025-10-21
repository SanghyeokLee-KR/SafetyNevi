package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import com.inha.pro.safetynevi.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DisasterService disasterService;
    private final MemberService memberService;

    // 원형 재난 시뮬레이션 (중심좌표 + 반경)
    @PostMapping("/simulate")
    public ResponseEntity<DisasterZoneResponse> createDisaster(
            @RequestParam double lat, @RequestParam double lon,
            @RequestParam String type, @RequestParam double radius,
            @RequestParam int durationMinutes
    ) {
        DisasterZone zone = disasterService.createCircleDisaster(lat, lon, type, radius, durationMinutes);
        return ResponseEntity.ok(DisasterZoneResponse.from(zone));
    }

    // 행정구역명으로 폴리곤 재난 시뮬레이션
    @PostMapping("/simulate-area")
    public ResponseEntity<DisasterZoneResponse> createAreaDisaster(
            @RequestParam String areaName,
            @RequestParam String type,
            @RequestParam int durationMinutes
    ) {
        DisasterZone zone = disasterService.createAreaDisaster(areaName, type, durationMinutes);
        return ResponseEntity.ok(DisasterZoneResponse.from(zone));
    }

    @DeleteMapping("/disaster/{id}")
    public ResponseEntity<String> deleteDisaster(@PathVariable Long id) {
        disasterService.deleteDisaster(id);
        return ResponseEntity.ok("삭제 성공");
    }

    @DeleteMapping("/member/{userId}")
    public ResponseEntity<String> kickMember(@PathVariable String userId) {
        log.info("[Admin] Force withdrawal request: ID={}", userId);

        if (memberService.isAdmin(userId)) { // 관리자끼리는 못 자름
            return ResponseEntity.badRequest().body("관리자 계정은 삭제할 수 없습니다.");
        }

        memberService.forceWithdraw(userId);
        return ResponseEntity.ok("삭제 성공");
    }
}