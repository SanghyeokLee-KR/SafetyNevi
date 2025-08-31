package com.inha.pro.safetynevi.entity.calamity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * 재난 구역(Disaster Zone) 엔티티
 * - 좌표/반경 기반의 원형 재난 또는 행정구역 기반 재난 정보 저장
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "DISASTER_ZONE")
public class DisasterZone {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "disaster_zone_seq")
    @SequenceGenerator(name = "disaster_zone_seq", sequenceName = "DISASTER_ZONE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "DISASTER_TYPE", nullable = false, length = 100)
    private String disasterType;

    // 원형 재난용 필드 (좌표 + 반경)
    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    @Column(name = "RADIUS")
    private Double radius;

    // 지역형 재난용 필드 (행정구역명)
    @Column(name = "AREA_NAME", length = 100)
    private String areaName;

    @Column(name = "START_TIME")
    private Instant startTime;

    @Column(name = "EXPIRY_TIME", nullable = false)
    private Instant expiryTime;

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = Instant.now();
        }
    }
}