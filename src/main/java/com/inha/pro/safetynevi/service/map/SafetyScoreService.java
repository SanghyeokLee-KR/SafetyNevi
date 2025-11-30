package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.FacilityRepository;
import com.inha.pro.safetynevi.dao.map.ShelterRepository;
import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;
import com.inha.pro.safetynevi.dto.map.SafetyScoreResponse;
import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.entity.Shelter;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 지점 기준 "대피 접근성" 점수. (구버전: 화면에 보이는 시설 개수 합산 → 줌·필터에 흔들림)
 *
 * 핵심 = 가장 가까운 운영 대피소까지의 거리. 여기에 1km 내 대피소 수(중복성),
 * 의료(병원)·대응(소방/경찰) 접근성을 더하고, 현재 위험구역 안이면 감점한다.
 * 규칙이 투명해 "왜 이 점수"를 그대로 설명할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class SafetyScoreService {

    private final ShelterRepository shelterRepository;
    private final FacilityRepository facilityRepository;
    private final DisasterService disasterService;

    private static final double BOX_DEG = 0.05;  // 약 ±5km (이 밖이면 대피 접근성 사실상 0)
    private static final int MAX_ROWS = 500;
    private static final double EARTH_R = 6_371_000;

    public SafetyScoreResponse evaluate(double lat, double lng) {
        PageRequest page = PageRequest.of(0, MAX_ROWS);
        double swLat = lat - BOX_DEG, swLng = lng - BOX_DEG, neLat = lat + BOX_DEG, neLng = lng + BOX_DEG;

        List<Shelter> shelters = shelterRepository.findOperationalInBounds(swLat, swLng, neLat, neLng, page);
        List<Facility> hospitals = facilityRepository.findFacilitiesInBounds("hospital", swLat, swLng, neLat, neLng, page);
        List<Facility> fire = facilityRepository.findFacilitiesInBounds("fire", swLat, swLng, neLat, neLng, page);
        List<Facility> police = facilityRepository.findFacilitiesInBounds("police", swLat, swLng, neLat, neLng, page);

        List<String> breakdown = new ArrayList<>();

        // 1) 가장 가까운 운영 대피소 + 1km 내 개수 (주축)
        Shelter nearestShelter = null;
        double nearestShelterD = Double.MAX_VALUE;
        int within1km = 0;
        for (Shelter s : shelters) {
            double d = haversine(lat, lng, s.getLatitude(), s.getLongitude());
            if (d < nearestShelterD) { nearestShelterD = d; nearestShelter = s; }
            if (d <= 1000) within1km++;
        }

        double base;
        if (nearestShelter == null) {
            base = 0;
            breakdown.add("반경 약 5km 내 운영 대피소 없음");
        } else {
            if (nearestShelterD <= 300) base = 60;
            else if (nearestShelterD >= 3000) base = 0;
            else base = 60 * (3000 - nearestShelterD) / 2700.0;
            breakdown.add(String.format("가장 가까운 대피소 %s (+%d)", fmtDist(nearestShelterD), Math.round(base)));
        }

        // 2) 중복성: 1km 내 추가 대피소 1곳당 +5 (최대 +20)
        double redundancy = 0;
        if (within1km > 1) {
            redundancy = Math.min((within1km - 1) * 5.0, 20);
            breakdown.add(String.format("1km 내 운영 대피소 %d곳 (+%d)", within1km, (int) redundancy));
        }

        // 3) 의료(병원) 접근성: 3km 내 거리 비례 최대 +10
        double nearestHospitalD = nearestDistance(lat, lng, hospitals);
        double medical = scaled(nearestHospitalD, 3000, 10);
        if (medical > 0) breakdown.add(String.format("가까운 병원 %s (+%d)", fmtDist(nearestHospitalD), Math.round(medical)));

        // 4) 대응(소방/경찰) 접근성: 3km 내 거리 비례 최대 +10
        double nearestResponseD = Math.min(nearestDistance(lat, lng, fire), nearestDistance(lat, lng, police));
        double response = scaled(nearestResponseD, 3000, 10);
        if (response > 0) breakdown.add(String.format("가까운 소방·경찰 %s (+%d)", fmtDist(nearestResponseD), Math.round(response)));

        // 5) 위험 노출: 현재 활성 위험구역 안이면 -30
        boolean hazardActive = false;
        String hazardName = null;
        for (DisasterZoneResponse z : disasterService.getActiveDisasterZones()) {
            if (z.getLatitude() == null || z.getLongitude() == null || z.getRadius() == null) continue;
            if (haversine(lat, lng, z.getLatitude(), z.getLongitude()) <= z.getRadius()) {
                hazardActive = true;
                hazardName = z.getAreaName();
                break;
            }
        }
        double hazardPenalty = hazardActive ? -30 : 0;
        if (hazardActive) breakdown.add("현재 위험구역 영향 (-30)");

        int score = (int) Math.round(clamp(base + redundancy + medical + response + hazardPenalty, 0, 100));

        SafetyScoreResponse.NearestShelter ns = (nearestShelter == null) ? null
                : new SafetyScoreResponse.NearestShelter(
                        nearestShelter.getName(),
                        (int) Math.round(nearestShelterD),
                        walkMinutes(nearestShelterD),
                        nearestShelter.getLatitude(),
                        nearestShelter.getLongitude());

        return new SafetyScoreResponse(score, grade(score), ns, within1km, hazardActive, hazardName, breakdown);
    }

    private double nearestDistance(double lat, double lng, List<Facility> list) {
        double min = Double.MAX_VALUE;
        for (Facility f : list) {
            double d = haversine(lat, lng, f.getLatitude(), f.getLongitude());
            if (d < min) min = d;
        }
        return min;
    }

    // 거리 d가 max 이내면 weight*(max-d)/max, 밖이면 0
    private double scaled(double d, double max, double weight) {
        if (d >= max) return 0;
        return weight * (max - d) / max;
    }

    private String grade(int score) {
        if (score >= 80) return "우수";
        if (score >= 60) return "양호";
        if (score >= 40) return "보통";
        return "주의";
    }

    // 직선거리 × 1.3(우회) ÷ 약 80m/분(도보 4.8km/h), 최소 1분
    private int walkMinutes(double meters) {
        return Math.max(1, (int) Math.round(meters * 1.3 / 80));
    }

    private String fmtDist(double m) {
        return (m < 1000) ? Math.round(m) + "m" : String.format("%.1fkm", m / 1000);
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
