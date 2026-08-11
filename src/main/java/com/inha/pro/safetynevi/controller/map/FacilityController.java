package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.FacilityDto;
import com.inha.pro.safetynevi.dto.map.FacilityMarkerDto;
import com.inha.pro.safetynevi.service.map.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    // 지도에 보이는 영역 안의 시설만, 마커에 필요한 필드만 조회
    @GetMapping
    public ResponseEntity<List<FacilityMarkerDto>> getFacilitiesInBounds(
            @RequestParam String type,
            @RequestParam double swLat, @RequestParam double swLng,
            @RequestParam double neLat, @RequestParam double neLng
    ) {
        return ResponseEntity.ok(
                facilityService.findFacilitiesInBounds(type, swLat, swLng, neLat, neLng)
        );
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getFacilityDetail(@PathVariable Long id) {
        Object detailDto = facilityService.findDetailById(id);
        return (detailDto != null) ? ResponseEntity.ok(detailDto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<FacilityDto>> searchFacilities(@RequestParam String keyword) {
        log.info("Search request: keyword={}", keyword);
        List<FacilityDto> results = facilityService.searchFacilitiesByName(keyword)
                .stream().map(FacilityDto::new).toList();
        return ResponseEntity.ok(results);
    }
}