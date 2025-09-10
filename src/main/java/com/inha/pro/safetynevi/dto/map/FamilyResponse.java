package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.member.Family;
import lombok.Builder;
import lombok.Getter;

/** 안심 연락처 응답 DTO */
@Getter
@Builder
public class FamilyResponse {

    private final Long id;
    private final String name;
    private final String phone;

    public static FamilyResponse from(Family family) {
        return FamilyResponse.builder()
                .id(family.getId())
                .name(family.getName())
                .phone(family.getPhone())
                .build();
    }
}
