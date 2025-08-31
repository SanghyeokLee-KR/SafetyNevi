package com.inha.pro.safetynevi.dto.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경로 추천 및 길찾기 결과 DTO
 * - 추천된 대피소 정보와 이동 소요 시간/거리 포함
 */
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
}