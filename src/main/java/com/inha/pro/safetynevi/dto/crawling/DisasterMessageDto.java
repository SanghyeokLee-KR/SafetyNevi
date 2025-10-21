package com.inha.pro.safetynevi.dto.crawling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class DisasterMessageDto {

    private String disasterType; // 예: 호우경보
    private String area;
    private String sentDate;
    private String content;
}
