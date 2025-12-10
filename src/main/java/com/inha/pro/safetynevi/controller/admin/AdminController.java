package com.inha.pro.safetynevi.controller.admin;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.inha.pro.safetynevi.dao.push.PushSubscriptionRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import com.inha.pro.safetynevi.service.map.RouteService;
import com.inha.pro.safetynevi.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DisasterService disasterService;
    private final MemberService memberService;
    private final RouteService routeService;
    private final PushSubscriptionRepository subscriptionRepository;

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

    // 발령 전 영향 미리보기: 이 원형 재난 반경에 닿는 대피소 수 + 알림 받을 구독자 수
    @GetMapping("/simulate/impact")
    public Map<String, Object> simulateImpact(
            @RequestParam double lat, @RequestParam double lon, @RequestParam double radius) {
        int shelters = routeService.countSheltersInRadius(lat, lon, radius);
        long subscribers = subscriptionRepository.count();
        return Map.of("shelterCount", shelters, "subscriberCount", subscribers);
    }

    // 온보딩 URL을 QR 코드 PNG로 (관리자가 시설/지역에 붙일 용도)
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@RequestParam String text, @RequestParam(defaultValue = "240") int size) {
        try {
            int s = Math.max(80, Math.min(600, size));
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, s, s);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(out.toByteArray());
        } catch (Exception e) {
            log.warn("QR 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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