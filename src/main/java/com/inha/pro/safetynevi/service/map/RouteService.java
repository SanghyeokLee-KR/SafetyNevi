package com.inha.pro.safetynevi.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.map.RouteDto;
import com.inha.pro.safetynevi.entity.Shelter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final ShelterRepository shelterRepository;

    // 평균 속도 설정
    private static final double WALK_SPEED_KMPH = 4.0; // 시속 4km
    private static final double CAR_SPEED_KMPH = 30.0; // 시속 30km (도심 재난 상황 가정)

    /**
     * 현재 위치 기준 최적의 대피소 3곳 추천
     */
    public List<RouteDto> getOptimalShelters(double currentLat, double currentLon) {
        // 1. 모든 대피소 가져오기 (실제 서비스에선 반경 5km 등으로 1차 필터링 권장)
        List<Shelter> allShelters = shelterRepository.findAll();

        // 2. DTO 변환 및 거리 계산
        List<RouteDto> candidates = allShelters.stream()
                .map(shelter -> {
                    double dist = calculateDistance(currentLat, currentLon, shelter.getLatitude(), shelter.getLongitude());
                    return RouteDto.builder()
                            .facilityId(shelter.getId())
                            .name(shelter.getName())
                            .type("shelter")
                            .latitude(shelter.getLatitude())
                            .longitude(shelter.getLongitude())
                            .operatingStatus(shelter.getOperatingStatus())
                            .maxCapacity(shelter.getMaxCapacity() != null ? shelter.getMaxCapacity() : 0)
                            .distanceMeter(dist)
                            .timeWalk(calculateTime(dist, WALK_SPEED_KMPH))
                            .timeCar(calculateTime(dist, CAR_SPEED_KMPH))
                            .build();
                })
                .collect(Collectors.toList());

        // 3. 추천 로직 적용
        List<RouteDto> results = new ArrayList<>();

        // [1순위] 최적 대피소 (운영중이고 + 수용인원 100명 이상 + 거리 가중치)
        // 간단하게: "운영중"인 곳 중에서 가장 가까운 곳
        candidates.stream()
                .filter(s -> isOperating(s.getOperatingStatus()))
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(best -> {
                    best.setRecommendationType("✅ 최적 추천 (운영중)");
                    results.add(best);
                });

        // [2순위] 최단 거리 (상태 무관, 급할 때 무조건 가까운 곳)
        candidates.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId()))) // 이미 뽑힌거 제외
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(nearest -> {
                    nearest.setRecommendationType("⚡ 최단 거리");
                    results.add(nearest);
                });

        // [3순위] 대형 대피소 (수용인원 많은 순, 거리가 좀 멀더라도)
        candidates.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId())))
                .sorted(Comparator.comparingInt(RouteDto::getMaxCapacity).reversed()) // 수용인원 내림차순
                .findFirst()
                .ifPresent(largest -> {
                    largest.setRecommendationType("🏢 대형 시설");
                    results.add(largest);
                });

        return results;
    }

    // --- Helper Methods ---

    // Haversine 공식 (직선 거리 계산, 단위: 미터)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // 미터 단위 반환
    }

    // 시간 계산 (분 단위)
    private int calculateTime(double distanceMeter, double speedKmph) {
        double speedMpm = (speedKmph * 1000) / 60; // 분당 미터 속도
        return (int) Math.ceil(distanceMeter / speedMpm);
    }

    private boolean isOperating(String status) {
        return status != null && (status.contains("정상") || status.contains("영업") || status.contains("운영"));
    }

    @Value("${api.kakao.restKey}") // application.properties에 있는 키 사용
    private String kakaoRestKey;

    private final WebClient webClient = WebClient.create();

    /**
     * 카카오 모빌리티 API를 호출하여 실제 경로(Vertex) 데이터를 가져옵니다.
     */
    public JsonNode getKakaoRoute(double startLat, double startLon, double endLat, double endLon) {
        String url = "https://apis-navi.kakaomobility.com/v1/directions"
                + "?origin=" + startLon + "," + startLat
                + "&destination=" + endLon + "," + endLat
                + "&priority=RECOMMEND"; // 추천 경로

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "KakaoAK " + kakaoRestKey)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 간단한 구현을 위해 동기식(block) 처리

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response);

        } catch (Exception e) {
            log.error("카카오 길찾기 API 호출 실패", e);
            return null;
        }
    }
}