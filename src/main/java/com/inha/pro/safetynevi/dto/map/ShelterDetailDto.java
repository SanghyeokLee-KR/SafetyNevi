package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Shelter;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대피소 상세 정보 DTO
 */
@Getter
@NoArgsConstructor
public class ShelterDetailDto {
    private Long id;
    private String type;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingStatus;
    private Double areaM2;          // 시설 면적
    private Integer maxCapacity;    // 최대 수용 인원
    private String locationType;    // 시설 구분 (지상/지하 등)

    public ShelterDetailDto(Shelter shelter) {
        this.id = shelter.getId();
        this.type = shelter.getType();
        this.name = shelter.getName();
        this.address = shelter.getAddress();
        this.latitude = shelter.getLatitude();
        this.longitude = shelter.getLongitude();
        this.operatingStatus = shelter.getOperatingStatus();
        this.areaM2 = shelter.getAreaM2();
        this.maxCapacity = shelter.getMaxCapacity();
        this.locationType = shelter.getLocationType();
    }
}