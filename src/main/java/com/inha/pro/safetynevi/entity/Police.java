package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 경찰서(Police) 상세 정보 엔티티
 * - Facility 상속
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "POLICE_DETAIL")
@DiscriminatorValue("police")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class Police extends Facility {

    @Column(name = "PHONE_NUMBER", length = 100)
    private String phoneNumber;

    @Column(name = "GUBUN", length = 100)
    private String gubun; // 지구대, 파출소 구분

    @Column(name = "SIDO_CHEONG", length = 100)
    private String sidoCheong; // 관할 지방청
}