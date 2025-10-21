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

    private static final double WALK_SPEED_KMPH = 4.0;
    private static final double CAR_SPEED_KMPH = 30.0; // 도심 재난상황 가정한 평균치

    // 현재 위치 기준 추천 대피소 3곳 뽑기
    public List<RouteDto> getOptimalShelters(double currentLat, double currentLon) {
        // 전국 풀스캔 대신 주변만 박스쿼리로 조회, 모자라면 범위 확대
        List<Shelter> nearbyShelters = findNearbyShelters(currentLat, currentLon);

        List<RouteDto> candidates = nearbyShelters.stream()
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

        List<RouteDto> results = new ArrayList<>();

        // 1순위: 운영중인 곳 중 제일 가까운 데
        candidates.stream()
                .filter(s -> isOperating(s.getOperatingStatus()))
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(best -> {
                    best.setRecommendationType("✅ 최적 추천 (운영중)");
                    results.add(best);
                });

        // 2순위: 운영상태 상관없이 그냥 최단거리 (급하면 가까운 게 최고)
        candidates.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId()))) // 이미 뽑힌건 빼고
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(nearest -> {
                    nearest.setRecommendationType("⚡ 최단 거리");
                    results.add(nearest);
                });

        // 3순위: 좀 멀어도 수용인원 큰 대형 시설
        candidates.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId())))
                .sorted(Comparator.comparingInt(RouteDto::getMaxCapacity).reversed())
                .findFirst()
                .ifPresent(largest -> {
                    largest.setRecommendationType("🏢 대형 시설");
                    results.add(largest);
                });

        return results;
    }

    // 현재 위치 주변 대피소를 박스(위경도 범위)로 조회한다. 반경을 점차 넓혀 후보를 확보하고,
    // 그래도 거의 없으면(극외딴 지역) 전체에서 찾는다.
    private List<Shelter> findNearbyShelters(double lat, double lon) {
        List<Shelter> found = new ArrayList<>();
        for (double radiusKm : new double[]{5, 20, 100}) {
            double latDelta = radiusKm / 111.0;                                   // 위도 1도 ≈ 111km
            double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat))); // 경도는 위도에 따라 보정
            found = shelterRepository.findAllInBounds(
                    lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta);
            if (found.size() >= 3) return found;
        }
        // 100km 안에도 3곳 미만이면: 후보가 있으면 그걸로, 아예 없으면 전체 폴백(드문 경우)
        return found.isEmpty() ? shelterRepository.findAll() : found;
    }

    // Haversine 공식으로 직선거리(미터) 계산
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }

    // 거리/속도로 소요시간(분) 계산
    private int calculateTime(double distanceMeter, double speedKmph) {
        double speedMpm = (speedKmph * 1000) / 60; // 분당 이동거리(m)
        return (int) Math.ceil(distanceMeter / speedMpm);
    }

    private boolean isOperating(String status) {
        return status != null && (status.contains("정상") || status.contains("영업") || status.contains("운영"));
    }

    @Value("${api.kakao.restKey}")
    private String kakaoRestKey;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 카카오 모빌리티에서 실제 도로 경로(꺾인 좌표들) 받아오기
    public JsonNode getKakaoRoute(double startLat, double startLon, double endLat, double endLon) {
        String url = "https://apis-navi.kakaomobility.com/v1/directions"
                + "?origin=" + startLon + "," + startLat
                + "&destination=" + endLon + "," + endLat
                + "&priority=RECOMMEND";

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "KakaoAK " + kakaoRestKey)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 호출부가 동기라 그냥 block

            return objectMapper.readTree(response);

        } catch (Exception e) {
            log.error("카카오 길찾기 API 호출 실패", e);
            return null;
        }
    }
}