package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.FacilityRepository;
import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.dto.map.SafetyScoreResponse;
import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.entity.Shelter;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyScoreServiceTest {

    @Mock ShelterRepository shelterRepository;
    @Mock FacilityRepository facilityRepository;
    @Mock DisasterService disasterService;
    @InjectMocks SafetyScoreService service;

    private static final double LAT = 37.5, LNG = 127.0;

    private Shelter shelter(String name, double lat, double lng) {
        Shelter s = new Shelter();
        s.setName(name); s.setLatitude(lat); s.setLongitude(lng);
        return s;
    }

    private void givenShelters(List<Shelter> shelters) {
        when(shelterRepository.findOperationalInBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(shelters);
        when(facilityRepository.findFacilitiesInBounds(anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.<Facility>of());
        when(disasterService.getActiveDisasterZones()).thenReturn(List.of());
    }

    @Test
    void nearbyShelterGivesHighScore() {
        givenShelters(List.of(shelter("가까운대피소", 37.5018, 127.0))); // 약 200m

        SafetyScoreResponse r = service.evaluate(LAT, LNG);

        assertEquals(60, r.score());          // base 60, 그 외 가산 없음
        assertEquals("양호", r.grade());
        assertNotNull(r.nearestShelter());
        assertTrue(r.nearestShelter().distanceM() < 300);
        assertFalse(r.hazardActive());
    }

    @Test
    void noShelterIsCaution() {
        givenShelters(List.<Shelter>of());

        SafetyScoreResponse r = service.evaluate(LAT, LNG);

        assertEquals(0, r.score());
        assertEquals("주의", r.grade());
        assertNull(r.nearestShelter());
    }

    @Test
    void insideHazardZoneAppliesPenalty() {
        when(shelterRepository.findOperationalInBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(shelter("가까운대피소", 37.5018, 127.0)));
        when(facilityRepository.findFacilitiesInBounds(anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.<Facility>of());
        when(disasterService.getActiveDisasterZones()).thenReturn(List.of(
                DisasterZoneResponse.builder()
                        .latitude(LAT).longitude(LNG).radius(1000.0).areaName("서울특별시 강남구").build()));

        SafetyScoreResponse r = service.evaluate(LAT, LNG);

        assertTrue(r.hazardActive());
        assertEquals("서울특별시 강남구", r.hazardName());
        assertEquals(30, r.score());          // base 60 - 위험구역 30
    }
}
