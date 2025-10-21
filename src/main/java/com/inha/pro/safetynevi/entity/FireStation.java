package com.inha.pro.safetynevi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

// 원본 CSV가 컬럼을 어긋나게 담고 있어서 getter 몇 개를 손봤다 (아래 참고)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "FIRE_STATION_DETAIL")
@DiscriminatorValue("fire")
@PrimaryKeyJoinColumn(name = "FACILITY_ID")
public class FireStation extends Facility {

    // PHONE_NUMBER_HQ 컬럼에 실제로는 주소가 들어있다
    @Column(name = "PHONE_NUMBER_HQ", length = 100)
    private String addressInPhoneColumn;

    @Column(name = "SUB_TYPE", length = 100)
    private String subType; // 119안전센터, 구조대 등

    // 예전 코드가 이 이름으로 부르고 있어서 남겨둠
    public String getPhoneNumberHq() {
        return this.addressInPhoneColumn;
    }

    // 주소가 엉뚱한 컬럼에 있어서 오버라이드로 맞춰준다
    @Override
    public String getAddress() {
        return this.addressInPhoneColumn;
    }

    // 전화번호 데이터가 없는 셋이라 그냥 null
    public String getPhoneNumber() {
        return null;
    }
}