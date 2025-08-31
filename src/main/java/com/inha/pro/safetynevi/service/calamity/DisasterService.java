package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dao.calamity.DisasterZoneRepository;
import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import com.inha.pro.safetynevi.exception.ResourceNotFoundException; // 🌟 커스텀 예외 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DisasterService {

    private final DisasterZoneRepository disasterZoneRepository;

    // 1. 원형 재난 생성
    public DisasterZone createCircleDisaster(double lat, double lon, String type, double radius, int durationMinutes) {
        DisasterZone zone = new DisasterZone();
        zone.setDisasterType(type);
        zone.setLatitude(lat);
        zone.setLongitude(lon);
        zone.setRadius(radius);

        Instant expiryTime = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        zone.setExpiryTime(expiryTime);

        log.info("🌍 [Service] 원형 재난 생성: {}", zone);
        return disasterZoneRepository.save(zone);
    }

    // 2. 지역(Polygon) 재난 생성
    public DisasterZone createAreaDisaster(String areaName, String type, int durationMinutes) {
        DisasterZone zone = new DisasterZone();
        zone.setDisasterType(type);
        zone.setAreaName(areaName);

        Instant expiryTime = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        zone.setExpiryTime(expiryTime);

        log.info("🏙️ [Service] 지역 재난 생성: {}", zone);
        return disasterZoneRepository.save(zone);
    }

    // 3. 🌟 [수정] 재난 삭제 (명시적 예외 처리)
    public void deleteDisaster(Long id) {
        // 먼저 조회하고, 없으면 404 예외를 던짐 -> GlobalExceptionHandler가 받음
        DisasterZone zone = disasterZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 재난 정보가 없습니다: " + id));

        disasterZoneRepository.delete(zone);
        log.info("🗑️ [Service] 재난 삭제 완료: ID={}", id);
    }

    // 4. 모든 재난 조회
    @Transactional(readOnly = true)
    public List<DisasterZone> findAll() {
        return disasterZoneRepository.findAll();
    }

    // 5. 현재 활성화된 재난만 조회
    @Transactional(readOnly = true)
    public List<DisasterZone> findActiveDisasters() {
        Instant now = Instant.now();
        return disasterZoneRepository.findAll().stream()
                .filter(zone -> zone.getExpiryTime() != null && zone.getExpiryTime().isAfter(now))
                .collect(Collectors.toList());
    }

    // 6. 전체 재난 수 조회 (Admin Dashboard용)
    @Transactional(readOnly = true)
    public long countDisasters() {
        return disasterZoneRepository.count();
    }
}