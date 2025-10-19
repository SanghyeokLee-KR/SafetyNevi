package com.inha.pro.safetynevi.dto.calamity;

import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/** 재난 구역 응답 DTO (발령/만료 시각 등 내부 필드 제외). Redis 분산 캐시 대상이라 Serializable. */
@Getter
@Builder
public class DisasterZoneResponse implements Serializable {

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
