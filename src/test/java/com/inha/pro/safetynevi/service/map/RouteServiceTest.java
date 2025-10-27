package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.map.RouteDto;
import com.inha.pro.safetynevi.entity.Shelter;
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
}
