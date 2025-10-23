package com.inha.pro.safetynevi.dto.crawling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class DisasterMessageDto {

    private String disasterType;   // 예: 호우경보
    private String emergencyLevel; // 공식 긴급단계: 위급재난/긴급재난/안전안내
    private String area;
    private String sentDate;
    private String content;
}
