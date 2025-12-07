package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.dto.map.RouteDto;
import com.inha.pro.safetynevi.entity.Shelter;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock ShelterRepository shelterRepository;
    @Mock DisasterService disasterService;
    @InjectMocks RouteService routeService;

    private Shelter shelter(long id, String name, double lat, double lon, String status, int capacity) {
        Shelter s = new Shelter();
        s.setId(id);
        s.setName(name);
        s.setLatitude(lat);
        s.setLongitude(lon);
        s.setOperatingStatus(status);
        s.setMaxCapacity(capacity);
        return s;
    }

    // 운영중 판정은 상태값 '사용중' 기준이어야 한다.
    // 옛 코드는 '정상/영업/운영'을 봐서, 실제 상태값('사용중')과 안 맞아 추천이 항상 빗나갔다.
    @Test
    void 운영중_추천은_사용중_상태인_대피소를_고른다() {
        double userLat = 37.5000, userLon = 127.0000;
        // '정상'이 가장 가깝지만 실제 운영 상태값이 아니므로 운영중 추천에서 빠져야 함
        Shelter normalClose = shelter(1L, "가까운정상", 37.5000, 127.0000, "정상", 50);
        Shelter closedMid   = shelter(2L, "사용중지",   37.5010, 127.0000, "사용중지", 50);
        Shelter openFar     = shelter(3L, "사용중먼곳", 37.5030, 127.0000, "사용중", 80);

        when(shelterRepository.findAllInBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(normalClose, closedMid, openFar));

        List<RouteDto> result = routeService.getOptimalShelters(userLat, userLon);

        RouteDto operating = result.stream()
                .filter(r -> r.getRecommendationType() != null && r.getRecommendationType().contains("운영중"))
                .findFirst()
                .orElseThrow();

        assertThat(operating.getOperatingStatus()).isEqualTo("사용중");
        assertThat(operating.getName()).isEqualTo("사용중먼곳");
    }

    // 재난 발령 시, 위험구역 '안'에 있는 대피소는 추천에서 빠지고 안전한 쪽이 남아야 한다.
    @Test
    void 재난시_위험구역_안의_대피소는_추천에서_빠진다() {
        double userLat = 37.5000, userLon = 127.0000;
        // 위험구역: 사용자 동쪽 (37.5000, 127.0050) 반경 300m
        DisasterZoneResponse hazard = DisasterZoneResponse.builder()
                .disasterType("fire").latitude(37.5000).longitude(127.0050).radius(300.0).build();
        when(disasterService.getActiveDisasterZones()).thenReturn(List.of(hazard));

        Shelter inside   = shelter(1L, "위험구역안", 37.5000, 127.0050, "사용중", 50); // 재난 중심 = 위험구역 안
        Shelter safeWest = shelter(2L, "안전한서쪽", 37.5000, 126.9950, "사용중", 50); // 반대편(서쪽), 경로가 재난 안 지남
        when(shelterRepository.findAllInBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(inside, safeWest));

        List<RouteDto> result = routeService.getOptimalShelters(userLat, userLon);

        assertThat(result).extracting(RouteDto::getName).contains("안전한서쪽");
        assertThat(result).extracting(RouteDto::getName).doesNotContain("위험구역안");
        assertThat(result).allMatch(RouteDto::isSafe);
    }

    // 점-선분 최단거리(경로가 위험구역을 지나는지 판정의 핵심) 기하 검증.
    @Test
    void distancePointToSegmentMeters_점에서_선분까지_최단거리() {
        double aLat = 37.500, aLon = 127.000, bLat = 37.510, bLon = 127.000; // 남북 약 1.1km 선분
        // 선분 위(중간점) → 거의 0
        assertThat(RouteService.distancePointToSegmentMeters(37.505, 127.000, aLat, aLon, bLat, bLon)).isLessThan(5.0);
        // 중간에서 경도 +0.001 (동쪽 약 88m)
        assertThat(RouteService.distancePointToSegmentMeters(37.505, 127.001, aLat, aLon, bLat, bLon)).isBetween(70.0, 105.0);
        // 선분에서 한참 남쪽 → 큼
        assertThat(RouteService.distancePointToSegmentMeters(37.480, 127.000, aLat, aLon, bLat, bLon)).isGreaterThan(2000.0);
    }
}
