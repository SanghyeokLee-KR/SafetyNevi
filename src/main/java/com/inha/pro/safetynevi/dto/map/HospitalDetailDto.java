package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Hospital;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 병원 상세 정보 DTO
 */
@Getter
@NoArgsConstructor
public class HospitalDetailDto {
    private Long id;
    private String type;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private String roadAddress;
    private String subType;         // 병원 종류 (종합병원, 의원 등)
    private Integer bedCount;       // 병상 수
    private Integer staffCount;     // 의료진 수
    private String operatingStatus; // 영업 상태

    public HospitalDetailDto(Hospital hospital) {
        this.id = hospital.getId();
        this.type = hospital.getType();
        this.name = hospital.getName();
        this.address = hospital.getAddress();
        this.latitude = hospital.getLatitude();
        this.longitude = hospital.getLongitude();
        this.phoneNumber = hospital.getPhoneNumber();
        this.roadAddress = hospital.getRoadAddress();
        this.subType = hospital.getSubType();
        this.bedCount = hospital.getBedCount();
        this.staffCount = hospital.getStaffCount();
        this.operatingStatus = hospital.getOperatingStatus();
    }
}