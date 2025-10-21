package com.inha.pro.safetynevi.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Python AI 서버 응답
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {
    private String disasterType;
    private String safety;
    private double confidence;
}