package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 소방서(FireStation) 상세 정보 엔티티
 * - Facility 상속
 * - 원본 CSV 데이터 매핑 이슈로 인해 일부 Getter 메서드 오버라이드 처리
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "FIRE_STATION_DETAIL")
@DiscriminatorValue("fire")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class FireStation extends Facility {

    // 실제 데이터베이스에는 이 컬럼에 '주소' 정보가 저장되어 있음
    @Column(name = "PHONE_NUMBER_HQ", length = 100)
    private String addressInPhoneColumn;

    @Column(name = "SUB_TYPE", length = 100)
    private String subType; // 119안전센터, 구조대 등

    // --- 데이터 보정 메서드 ---

    // 레거시 코드 호환성 유지
    public String getPhoneNumberHq() {
        return this.addressInPhoneColumn;
    }

    // 부모의 getAddress를 오버라이드하여 실제 주소가 저장된 필드를 반환
    @Override
    public String getAddress() {
        return this.addressInPhoneColumn;
    }

    // 실제 전화번호 데이터가 없으므로 null 반환
    public String getPhoneNumber() {
        return null;
    }
}