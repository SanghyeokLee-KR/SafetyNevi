package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.service.map.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 개인화 지도 데이터 API (즐겨찾기, 안심 연락망)
 */
@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapApiController {

    private final MapService mapService;

    // 내 장소 전체 조회
    @GetMapping("/my-places")
    public ResponseEntity<?> getMyPlaces(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(mapService.getMyAllPlaces(user.getUsername()));
    }

    // 주요 장소(집/회사) 등록
    @PostMapping("/special-place")
    public ResponseEntity<?> saveSpecialPlace(@RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            mapService.saveSpecialPlace(
                    user.getUsername(),
                    (String) payload.get("type"),
                    (String) payload.get("address"),
                    Double.parseDouble(payload.get("latitude").toString()),
                    Double.parseDouble(payload.get("longitude").toString())
            );
            return ResponseEntity.ok("saved");
        } catch (Exception e) { return ResponseEntity.badRequest().build(); }
    }

    // 즐겨찾기 추가
    @PostMapping("/favorite")
    public ResponseEntity<?> addFavorite(@RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            mapService.addFavorite(
                    user.getUsername(),
                    (String) payload.get("name"),
                    (String) payload.get("address"),
                    Double.parseDouble(payload.get("latitude").toString()),
                    Double.parseDouble(payload.get("longitude").toString())
            );
            return ResponseEntity.ok("added");
        } catch (Exception e) { return ResponseEntity.badRequest().build(); }
    }

    // 장소 삭제
    @DeleteMapping("/place/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable Long id) {
        mapService.deletePlace(id);
        return ResponseEntity.ok("deleted");
    }

    // --- 가족/지인 연락처 관리 ---

    @GetMapping("/family")
    public ResponseEntity<?> getFamilyList(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mapService.getFamilyList(user.getUsername()));
    }

    @PostMapping("/family")
    public ResponseEntity<?> addFamily(@RequestBody Map<String, String> payload, @AuthenticationPrincipal User user) {
        mapService.addFamily(user.getUsername(), payload.get("name"), payload.get("phone"));
        return ResponseEntity.ok("added");
    }

    @DeleteMapping("/family/{id}")
    public ResponseEntity<?> deleteFamily(@PathVariable Long id) {
        mapService.deleteFamily(id);
        return ResponseEntity.ok("deleted");
    }
}