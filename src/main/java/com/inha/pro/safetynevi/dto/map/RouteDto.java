package com.inha.pro.safetynevi.dto.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDto {
    private Long facilityId;
    private String name;
    private String type;
    private double latitude;
    private double longitude;

    // 추천 사유 (예: "최단 거리", "최적 수용", "안전 추천")
    private String recommendationType;

    // 경로 계산 결과
    private double distanceMeter; // 이동 거리 (m)
    private int timeWalk;         // 도보 예상 소요 시간 (분)
    private int timeCar;          // 차량 예상 소요 시간 (분)

    // 시설 부가 정보
    private String operatingStatus;
    private Integer maxCapacity;

    // 재난 상황에서 이 대피소까지의 경로가 위험구역을 지나지 않는지(=안전한 대피로). 재난 없으면 기본 true.
    private boolean safe;
}