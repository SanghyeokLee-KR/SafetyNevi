package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.dao.map.*;
import com.inha.pro.safetynevi.dto.map.*;
import com.inha.pro.safetynevi.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시설물(Facility) 조회 서비스
 * - 지도 내 범위 검색(Bounds Search) 및 상세 정보 조회
 * - 다형성을 활용하여 각 시설 타입(병원, 소방서 등)에 맞는 DTO 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final HospitalRepository hospitalRepository;
    private final ShelterRepository shelterRepository;

    // 지도 영역 내 시설물 검색
    public List<FacilityDto> findFacilitiesInBounds(String type, double swLat, double swLng, double neLat, double neLng) {
        List<Facility> facilities = new ArrayList<>();

        switch (type) {
            case "police":
            case "fire":
                // 경찰/소방서는 상태 필터링 없이 전체 조회
                facilities.addAll(facilityRepository.findFacilitiesInBounds(type, swLat, swLng, neLat, neLng));
                break;
            case "hospital":
                // 병원은 '운영중'인 시설만 조회
                facilities.addAll(hospitalRepository.findOperationalInBounds(swLat, swLng, neLat, neLng));
                break;
            case "shelter":
                // 대피소는 사용 가능한 시설만 조회
                facilities.addAll(shelterRepository.findOperationalInBounds(swLat, swLng, neLat, neLng));
                break;
            case "etc":
                // 기타: 운영 중단된 병원 및 대피소 포함
                facilities.addAll(hospitalRepository.findNonOperationalInBounds(swLat, swLng, neLat, neLng));
                facilities.addAll(shelterRepository.findNonOperationalInBounds(swLat, swLng, neLat, neLng));
                break;
        }

        return facilities.stream()
                .map(FacilityDto::new)
                .collect(Collectors.toList());
    }

    // 시설 상세 정보 조회 (다형성 처리)
    public Object findDetailById(Long id) {
        Facility facility = facilityRepository.findById(id).orElse(null);

        if (facility == null) return null;

        if (facility instanceof Police) return new PoliceDetailDto((Police) facility);
        if (facility instanceof FireStation) return new FireStationDetailDto((FireStation) facility);
        if (facility instanceof Hospital) return new HospitalDetailDto((Hospital) facility);
        if (facility instanceof Shelter) return new ShelterDetailDto((Shelter) facility);

        return new FacilityDto(facility);
    }

    // 시설명 검색
    public List<Facility> searchFacilitiesByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        return facilityRepository.findByNameContaining(keyword.trim());
    }
}