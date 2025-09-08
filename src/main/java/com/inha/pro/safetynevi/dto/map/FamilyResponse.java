package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.member.Family;
import lombok.Builder;
import lombok.Getter;

/**
 * 안심 연락처(가족/지인) 응답 DTO
 * - 소유자 userId는 요청자 본인이라 내려보내지 않는다.
 */
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
