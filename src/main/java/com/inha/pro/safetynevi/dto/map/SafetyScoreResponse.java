package com.inha.pro.safetynevi.dto.map;

import java.util.List;

/**
 * 한 지점의 "대피 접근성" 평가 결과.
 * 화면 시설 카운트가 아니라, 지점 기준으로 가장 가까운 운영 대피소까지의 거리를 핵심으로
 * 의료·대응 접근성과 현재 위험구역 노출까지 반영한 0~100 점수.
 */
public record SafetyScoreResponse(
        int score,                       // 0~100 (대피 접근성)
        String grade,                    // 우수 / 양호 / 보통 / 주의 (중립 표현)
        NearestShelter nearestShelter,   // 가장 가까운 운영 대피소 (없으면 null)
        int shelterCount1km,             // 1km 내 운영 대피소 수
        boolean hazardActive,            // 현재 활성 위험구역 안인지
        String hazardName,               // 위험구역 지역명 (없으면 null)
        List<String> breakdown           // "왜 이 점수" 설명 줄들
) {
    /** 가장 가까운 운영 대피소: 이름 · 직선거리(m) · 도보 추정시간(분) · 좌표(지도 이동용) */
    public record NearestShelter(String name, int distanceM, int walkMinutes, double lat, double lng) {}
}
