package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.FireStation;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FireStationDetailDto {
    private Long id;
    private String type;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumberHq;
    private String subType;

    public FireStationDetailDto(FireStation fireStation) {
        this.id = fireStation.getId();
        this.type = fireStation.getType();
        this.name = fireStation.getName();
        this.address = fireStation.getAddress();
        this.latitude = fireStation.getLatitude();
        this.longitude = fireStation.getLongitude();
        this.phoneNumberHq = fireStation.getPhoneNumberHq();
        this.subType = fireStation.getSubType();
    }
}