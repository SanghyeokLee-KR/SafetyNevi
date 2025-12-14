package com.inha.pro.safetynevi.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.dto.map.RouteDto;
import com.inha.pro.safetynevi.entity.Shelter;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
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
    private final DisasterService disasterService;

    private static final double WALK_SPEED_KMPH = 4.0;
    private static final double CAR_SPEED_KMPH = 30.0; // 도심 재난상황 가정한 평균치

    // 현재 위치 기준 추천 대피소 3곳 뽑기. 활성 재난이 있으면 위험구역을 피한 '안전한' 대피소·경로를 우선한다.
    public List<RouteDto> getOptimalShelters(double currentLat, double currentLon) {
        // 전국 풀스캔 대신 주변만 박스쿼리로 조회, 모자라면 범위 확대
        List<Shelter> nearbyShelters = findNearbyShelters(currentLat, currentLon);
        List<DisasterZoneResponse> hazards = activeCircleHazards();   // 위치·반경 있는 원형 재난만
        boolean hasHazard = !hazards.isEmpty();

        // 위험구역 '안'에 있는 대피소는 후보에서 제외, 거기로 대피시키면 안 된다. (전부 위험구역이면 어쩔 수 없이 유지)
        List<Shelter> usable = nearbyShelters;
        if (hasHazard) {
            List<Shelter> outside = nearbyShelters.stream()
                    .filter(s -> !insideAnyHazard(s.getLatitude(), s.getLongitude(), hazards))
                    .collect(Collectors.toList());
            if (!outside.isEmpty()) usable = outside;
        }

        List<RouteDto> candidates = usable.stream()
                .map(shelter -> {
                    double dist = calculateDistance(currentLat, currentLon, shelter.getLatitude(), shelter.getLongitude());
                    boolean safePath = !hasHazard
                            || !pathCrossesAnyHazard(currentLat, currentLon, shelter.getLatitude(), shelter.getLongitude(), hazards);
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
                            .safe(safePath)
                            .build();
                })
                .collect(Collectors.toList());

        // 재난 시: 경로가 위험구역을 안 지나는 후보가 하나라도 있으면 그쪽만 추천 풀로 (없으면 전체, 포위된 상황)
        List<RouteDto> pool = candidates;
        if (hasHazard) {
            List<RouteDto> safePool = candidates.stream().filter(RouteDto::isSafe).collect(Collectors.toList());
            if (!safePool.isEmpty()) pool = safePool;
        }
        final List<RouteDto> poolF = pool;

        List<RouteDto> results = new ArrayList<>();

        // 1순위: 운영중인 곳 중 제일 가까운 데
        poolF.stream()
                .filter(s -> isOperating(s.getOperatingStatus()))
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(best -> {
                    best.setRecommendationType(hasHazard ? "✅ 안전 대피소 (위험구역 우회)" : "✅ 최적 추천 (운영중)");
                    results.add(best);
                });

        // 2순위: 운영상태 상관없이 그냥 최단거리 (급하면 가까운 게 최고)
        poolF.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId()))) // 이미 뽑힌건 빼고
                .min(Comparator.comparingDouble(RouteDto::getDistanceMeter))
                .ifPresent(nearest -> {
                    nearest.setRecommendationType(hasHazard ? "⚡ 최단 안전경로" : "⚡ 최단 거리");
                    results.add(nearest);
                });

        // 3순위: 좀 멀어도 수용인원 큰 대형 시설
        poolF.stream()
                .filter(s -> results.stream().noneMatch(r -> r.getFacilityId().equals(s.getFacilityId())))
                .sorted(Comparator.comparingInt(RouteDto::getMaxCapacity).reversed())
                .findFirst()
                .ifPresent(largest -> {
                    largest.setRecommendationType("🏢 대형 시설");
                    results.add(largest);
                });

        return results;
    }

    // 반경(원형 재난) 안에 들어오는 대피소 수, 관리자 영향 미리보기용.
    public int countSheltersInRadius(double lat, double lon, double radiusMeters) {
        double radiusKm = radiusMeters / 1000.0;
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        List<Shelter> box = shelterRepository.findAllInBounds(lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta);
        return (int) box.stream()
                .filter(s -> calculateDistance(lat, lon, s.getLatitude(), s.getLongitude()) <= radiusMeters)
                .count();
    }

    // 현재 활성 재난 중 위치·반경이 있는 '원형' 재난만. (지역(폴리곤) 재난은 좌표 우회가 모호해 제외) 조회 실패해도 길찾기는 동작.
    private List<DisasterZoneResponse> activeCircleHazards() {
        try {
            return disasterService.getActiveDisasterZones().stream()
                    .filter(z -> z.getLatitude() != null && z.getLongitude() != null
                            && z.getRadius() != null && z.getRadius() > 0)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("재난 구역 조회 실패, 재난 미반영으로 길찾기 진행: {}", e.getMessage());
            return List.of();
        }
    }

    // 지점이 어느 위험구역(원) 안에 있는가
    private boolean insideAnyHazard(double lat, double lon, List<DisasterZoneResponse> hazards) {
        for (DisasterZoneResponse z : hazards) {
            if (calculateDistance(lat, lon, z.getLatitude(), z.getLongitude()) <= z.getRadius()) return true;
        }
        return false;
    }

    // 출발→대피소 직선 경로가 위험구역(원)을 통과하는가 (점-선분 최단거리 < 반경)
    private boolean pathCrossesAnyHazard(double sLat, double sLon, double eLat, double eLon, List<DisasterZoneResponse> hazards) {
        for (DisasterZoneResponse z : hazards) {
            if (distancePointToSegmentMeters(z.getLatitude(), z.getLongitude(), sLat, sLon, eLat, eLon) <= z.getRadius()) return true;
        }
        return false;
    }

    // 점 P에서 선분 AB까지의 최단거리(m). 단거리라 등거리 직사각 근사(위도별 경도 보정). (테스트용 package-private static)
    static double distancePointToSegmentMeters(double pLat, double pLon, double aLat, double aLon, double bLat, double bLon) {
        double mPerDegLat = 111_320.0;
        double mPerDegLon = 111_320.0 * Math.cos(Math.toRadians((aLat + bLat) / 2.0));
        double px = pLon * mPerDegLon, py = pLat * mPerDegLat;
        double ax = aLon * mPerDegLon, ay = aLat * mPerDegLat;
        double bx = bLon * mPerDegLon, by = bLat * mPerDegLat;
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = (len2 == 0) ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * dx, cy = ay + t * dy;
        return Math.hypot(px - cx, py - cy);
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
        // 대피소 상태값은 '사용중'/'사용중지'/'일시중지' (CSV 기준). '사용중'이면서 '중지' 아닌 것만 운영중.
        if (status == null) return false;
        return status.contains("사용중") && !status.contains("중지");
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
                    .block(java.time.Duration.ofSeconds(5)); // 동기 호출 + 타임아웃(API 행 시 스레드 점유 방지)

            return objectMapper.readTree(response);

        } catch (Exception e) {
            log.error("카카오 길찾기 API 호출 실패", e);
            return null;
        }
    }
}