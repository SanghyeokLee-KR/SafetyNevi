package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.dao.map.*;
import com.inha.pro.safetynevi.dto.map.*;
import com.inha.pro.safetynevi.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final HospitalRepository hospitalRepository;
    private final ShelterRepository shelterRepository;

    // 한 번에 그릴 시설 수 상한 (지도가 수만 개 마커를 만들지 않도록)
    private static final Pageable BOUNDS_LIMIT = PageRequest.of(0, 1500);
    private static final Pageable SEARCH_LIMIT = PageRequest.of(0, 50);

    public List<FacilityDto> findFacilitiesInBounds(String type, double swLat, double swLng, double neLat, double neLng) {
        List<Facility> facilities = new ArrayList<>();

        switch (type) {
            case "police":
            case "fire":
                facilities.addAll(facilityRepository.findFacilitiesInBounds(type, swLat, swLng, neLat, neLng, BOUNDS_LIMIT));
                break;
            case "hospital":
                facilities.addAll(hospitalRepository.findOperationalInBounds(swLat, swLng, neLat, neLng, BOUNDS_LIMIT));
                break;
            case "shelter":
                facilities.addAll(shelterRepository.findOperationalInBounds(swLat, swLng, neLat, neLng, BOUNDS_LIMIT));
                break;
            case "etc":
                facilities.addAll(hospitalRepository.findNonOperationalInBounds(swLat, swLng, neLat, neLng, BOUNDS_LIMIT));
                facilities.addAll(shelterRepository.findNonOperationalInBounds(swLat, swLng, neLat, neLng, BOUNDS_LIMIT));
                break;
        }

        return facilities.stream()
                .map(FacilityDto::new)
                .collect(Collectors.toList());
    }

    // 실제 타입에 맞는 상세 DTO로 내려줌
    public Object findDetailById(Long id) {
        Facility facility = facilityRepository.findById(id).orElse(null);

        if (facility == null) return null;

        if (facility instanceof Police) return new PoliceDetailDto((Police) facility);
        if (facility instanceof FireStation) return new FireStationDetailDto((FireStation) facility);
        if (facility instanceof Hospital) return new HospitalDetailDto((Hospital) facility);
        if (facility instanceof Shelter) return new ShelterDetailDto((Shelter) facility);

        return new FacilityDto(facility);
    }

    public List<Facility> searchFacilitiesByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        return facilityRepository.findByNameContaining(keyword.trim(), SEARCH_LIMIT);
    }
}