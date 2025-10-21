package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.entity.Hospital;
import com.inha.pro.safetynevi.entity.Shelter;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 지도 마커용 시설 요약. 모든 시설 타입을 한 형태로 합친다
@Getter
@NoArgsConstructor
public class FacilityDto {

    private Long id;
    private String type; // police, fire, hospital, shelter
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingStatus; // 영업/폐업 등
    private Integer maxCapacity;    // 대피소만 해당

    public FacilityDto(Facility facility) {
        this.id = facility.getId();
        this.type = facility.getType();
        this.name = facility.getName();
        this.address = facility.getAddress();
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