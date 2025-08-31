package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.entity.Hospital;
import com.inha.pro.safetynevi.entity.Shelter;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지도 마커용 시설물 요약 정보 DTO
 * - 모든 시설(경찰, 소방, 병원, 대피소)의 공통 필드 및 일부 특화 필드 포함
 */
@Getter
@NoArgsConstructor
public class FacilityDto {

    private Long id;
    private String type;            // 시설 유형 (police, fire, hospital, shelter)
    private String name;
    private double latitude;
    private double longitude;
    private String operatingStatus; // 운영 상태 (영업/폐업 등)
    private Integer maxCapacity;    // 수용 가능 인원 (대피소 전용)

    public FacilityDto(Facility facility) {
        this.id = facility.getId();
        this.type = facility.getType();
        this.name = facility.getName();
        this.latitude = facility.getLatitude();
        this.longitude = facility.getLongitude();

        if (facility instanceof Hospital) {
            this.operatingStatus = ((Hospital) facility).getOperatingStatus();
            this.maxCapacity = 0;
        } else if (facility instanceof Shelter) {
            this.operatingStatus = ((Shelter) facility).getOperatingStatus();
            this.maxCapacity = ((Shelter) facility).getMaxCapacity();
        } else {
            this.operatingStatus = "N/A";
            this.maxCapacity = 0;
        }
    }
}