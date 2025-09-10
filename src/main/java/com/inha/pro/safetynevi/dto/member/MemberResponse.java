package com.inha.pro.safetynevi.dto.member;

import com.inha.pro.safetynevi.entity.member.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 회원 정보 응답 DTO (비밀번호·보안답변 등 민감 정보 제외) */
@Getter
@Builder
public class MemberResponse {

    private final String userId;
    private final String email;
    private final String name;
    private final String nickname;
    private final String address;
    private final String detailAddress;
    private final String areaName;
    private final Double latitude;
    private final Double longitude;
    private final String emergencyPhone;
    private final Integer pwQuestion;
    private final LocalDateTime joinDate;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .userId(member.getUserId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .address(member.getAddress())
                .detailAddress(member.getDetailAddress())
                .areaName(member.getAreaName())
                .latitude(member.getLatitude())
                .longitude(member.getLongitude())
                .emergencyPhone(member.getEmergencyPhone())
                .pwQuestion(member.getPwQuestion())
                .joinDate(member.getJoinDate())
                .build();
    }
}
