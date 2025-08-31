package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 병원(Hospital) 상세 정보 엔티티
 * - Facility 상속
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "HOSPITAL_DETAIL")
@DiscriminatorValue("hospital")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class Hospital extends Facility {

    @Column(name = "PHONE_NUMBER", length = 100)
    private String phoneNumber;

    @Column(name = "ROAD_ADDRESS", length = 1000)
    private String roadAddress;

    @Column(name = "SUB_TYPE", length = 100)
    private String subType; // 종합병원, 의원, 보건소 등

    @Column(name = "BED_COUNT")
    private Integer bedCount;

    @Column(name = "STAFF_COUNT")
    private Integer staffCount;

    @Column(name = "OPERATING_STATUS", length = 100)
    private String operatingStatus;
}