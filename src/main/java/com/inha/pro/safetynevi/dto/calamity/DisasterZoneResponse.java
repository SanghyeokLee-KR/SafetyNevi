package com.inha.pro.safetynevi.dto.calamity;

import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import lombok.Builder;
import lombok.Getter;

/**
 * 재난 구역 응답 DTO
 * - 지도·관리자 화면에 필요한 값만 노출한다. (내부 발령/만료 시각은 제외)
 */
@Getter
@Builder
public class DisasterZoneResponse {

    private final Long id;
    private final String disasterType;
    private final Double latitude;
    private final Double longitude;
    private final Double radius;
    private final String areaName;

    public static DisasterZoneResponse from(DisasterZone zone) {
        return DisasterZoneResponse.builder()
                .id(zone.getId())
                .disasterType(zone.getDisasterType())
                .latitude(zone.getLatitude())
                .longitude(zone.getLongitude())
                .radius(zone.getRadius())
                .areaName(zone.getAreaName())
                .build();
    }
}
