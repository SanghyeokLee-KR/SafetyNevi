package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 회원(Member) 엔티티
 * - 로그인 정보, 개인정보, 위치 설정 값 저장
 */
@Entity
@Table(name = "SAFETY_MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(name = "DETAIL_ADDRESS", length = 300)
    private String detailAddress;

    @Column(name = "AREA_NAME", length = 100)
    private String areaName;

    // 내 장소(집) 좌표 (지도 중심점)
    @Column(name = "LATITUDE", columnDefinition = "NUMBER(10, 7)")
    private Double latitude;

    @Column(name = "LONGITUDE", columnDefinition = "NUMBER(10, 7)")
    private Double longitude;

    @Column(name = "EMERGENCY_PHONE", length = 20)
    private String emergencyPhone;

    // 비밀번호 찾기 질문/답변
    @Column(name = "PW_QUESTION", nullable = false)
    private Integer pwQuestion;

    @Column(name = "PW_ANSWER", nullable = false, length = 200)
    private String pwAnswer;

    @CreationTimestamp
    @Column(name = "JOIN_DATE", updatable = false)
    private LocalDateTime joinDate;

    // --- 비즈니스 로직 메서드 ---

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateInfo(String nickname, String emergencyPhone, String address, String detailAddress) {
        this.nickname = nickname;
        this.emergencyPhone = emergencyPhone;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    public void setAddress(String address) { this.address = address; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}