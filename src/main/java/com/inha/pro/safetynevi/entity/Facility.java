package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 시설물(Facility) 최상위 엔티티
 * - 상속 관계 매핑(InheritanceType.JOINED)을 사용하여 공통 속성(이름, 주소, 좌표) 관리
 * - 구분 컬럼(TYPE)을 통해 하위 엔티티(경찰, 소방, 병원, 대피소) 식별
 */
@Getter
@Setter
@Entity
@Table(name = "FACILITY")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TYPE")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "facility_seq")
    @SequenceGenerator(name = "facility_seq", sequenceName = "FACILITY_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ADDRESS", length = 1000)
    private String address;

    @Column(name = "LATITUDE", nullable = false)
    private double latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private double longitude;

    // 읽기 전용 구분자 (Insert/Update 불가)
    @Column(name = "TYPE", insertable = false, updatable = false, nullable = false)
    private String type;
}