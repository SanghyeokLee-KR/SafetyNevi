package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Police;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PoliceDetailDto {
    private Long id;
    private String type;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private String gubun;
    private String sidoCheong;

    public PoliceDetailDto(Police police) {
        this.id = police.getId();
        this.type = police.getType();
        this.name = police.getName();
        this.address = police.getAddress();
        this.latitude = police.getLatitude();
        this.longitude = police.getLongitude();
        this.phoneNumber = police.getPhoneNumber();
        this.gubun = police.getGubun();
        this.sidoCheong = police.getSidoCheong();
    }
}