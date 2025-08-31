package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.FacilityDto;
import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.service.map.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 시설물(Facility) 데이터 조회 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    // 지도 영역(Bounds) 내 시설물 조회
    @GetMapping
    public ResponseEntity<List<FacilityDto>> getFacilitiesInBounds(
            @RequestParam String type,
            @RequestParam double swLat, @RequestParam double swLng,
            @RequestParam double neLat, @RequestParam double neLng
    ) {
        return ResponseEntity.ok(
                facilityService.findFacilitiesInBounds(type, swLat, swLng, neLat, neLng)
        );
    }

    // 시설 상세 정보 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getFacilityDetail(@PathVariable Long id) {
        Object detailDto = facilityService.findDetailById(id);
        return (detailDto != null) ? ResponseEntity.ok(detailDto) : ResponseEntity.notFound().build();
    }

    // 시설명 키워드 검색
    @GetMapping("/search")
    public ResponseEntity<List<Facility>> searchFacilities(@RequestParam String keyword) {
        log.info("Search request: keyword={}", keyword);
        return ResponseEntity.ok(facilityService.searchFacilitiesByName(keyword));
    }
}