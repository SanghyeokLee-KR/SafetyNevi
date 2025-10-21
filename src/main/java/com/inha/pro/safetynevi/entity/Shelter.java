package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "SHELTER_DETAIL")
@DiscriminatorValue("shelter")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class Shelter extends Facility {

    @Column(name = "OPERATING_STATUS", length = 100)
    private String operatingStatus;

    @Column(name = "AREA_M2")
    private Double areaM2; // 제곱미터

    @Column(name = "MAX_CAPACITY")
    private Integer maxCapacity;

    @Column(name = "LOCATION_TYPE", length = 100)
    private String locationType; // 지하/지상 등
}