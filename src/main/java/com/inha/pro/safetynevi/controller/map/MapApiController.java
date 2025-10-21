package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.FamilyResponse;
import com.inha.pro.safetynevi.dto.map.FavoritePlaceResponse;
import com.inha.pro.safetynevi.service.map.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapApiController {

    private final MapService mapService;

    @GetMapping("/my-places")
    public ResponseEntity<?> getMyPlaces(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        List<FavoritePlaceResponse> places = mapService.getMyAllPlaces(user.getUsername())
                .stream().map(FavoritePlaceResponse::from).toList();
        return ResponseEntity.ok(places);
    }

    // 집/회사 같은 주요 장소
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

    @DeleteMapping("/place/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        mapService.deletePlace(user.getUsername(), id);
        return ResponseEntity.ok("deleted");
    }

    // 여기부터 가족/지인 연락처
    @GetMapping("/family")
    public ResponseEntity<?> getFamilyList(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        List<FamilyResponse> families = mapService.getFamilyList(user.getUsername())
                .stream().map(FamilyResponse::from).toList();
        return ResponseEntity.ok(families);
    }

    @PostMapping("/family")
    public ResponseEntity<?> addFamily(@RequestBody Map<String, String> payload, @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        mapService.addFamily(user.getUsername(), payload.get("name"), payload.get("phone"));
        return ResponseEntity.ok("added");
    }

    @DeleteMapping("/family/{id}")
    public ResponseEntity<?> deleteFamily(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        mapService.deleteFamily(user.getUsername(), id);
        return ResponseEntity.ok("deleted");
    }
}