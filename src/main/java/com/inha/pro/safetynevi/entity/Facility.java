package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// 경찰/소방/병원/대피소의 공통 부모. JOINED 상속, TYPE 컬럼으로 구분
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