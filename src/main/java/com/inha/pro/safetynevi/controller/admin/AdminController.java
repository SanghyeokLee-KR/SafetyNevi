package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import com.inha.pro.safetynevi.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용 기능 API 컨트롤러
 * - 재난 시뮬레이션 생성/종료
 * - 회원 강제 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DisasterService disasterService;
    private final MemberService memberService;

    // 원형(Circle) 재난 시뮬레이션 생성 (위도, 경도, 반경)
    @PostMapping("/simulate")
    public ResponseEntity<DisasterZone> createDisaster(
            @RequestParam double lat, @RequestParam double lon,
            @RequestParam String type, @RequestParam double radius,
            @RequestParam int durationMinutes
    ) {
        DisasterZone zone = disasterService.createCircleDisaster(lat, lon, type, radius, durationMinutes);
        return ResponseEntity.ok(zone);
    }

    // 지역(Polygon) 기반 재난 시뮬레이션 생성 (행정구역명)
    @PostMapping("/simulate-area")
    public ResponseEntity<DisasterZone> createAreaDisaster(
            @RequestParam String areaName,
            @RequestParam String type,
            @RequestParam int durationMinutes
    ) {
        DisasterZone zone = disasterService.createAreaDisaster(areaName, type, durationMinutes);
        return ResponseEntity.ok(zone);
    }

    // 재난 상황 종료 및 삭제
    @DeleteMapping("/disaster/{id}")
    public ResponseEntity<String> deleteDisaster(@PathVariable Long id) {
        disasterService.deleteDisaster(id);
        return ResponseEntity.ok("삭제 성공");
    }

    // 회원 강제 탈퇴 처리
    @DeleteMapping("/member/{userId}")
    public ResponseEntity<String> kickMember(@PathVariable String userId) {
        log.info("[Admin] Force withdrawal request: ID={}", userId);

        if("admin".equals(userId)) {
            return ResponseEntity.badRequest().body("관리자 계정은 삭제할 수 없습니다.");
        }

        memberService.forceWithdraw(userId);
        return ResponseEntity.ok("삭제 성공");
    }
}