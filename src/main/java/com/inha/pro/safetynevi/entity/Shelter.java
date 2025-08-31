package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 대피소(Shelter) 상세 정보 엔티티
 * - Facility 상속
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "SHELTER_DETAIL")
@DiscriminatorValue("shelter")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class Shelter extends Facility {

    @Column(name = "OPERATING_STATUS", length = 100)
    private String operatingStatus; // 운영 여부

    @Column(name = "AREA_M2")
    private Double areaM2;          // 시설 면적

    @Column(name = "MAX_CAPACITY")
    private Integer maxCapacity;    // 최대 수용 인원

    @Column(name = "LOCATION_TYPE", length = 100)
    private String locationType;    // 시설 구분 (지하/지상 등)
}