package com.inha.pro.safetynevi.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * AI 분석 응답 DTO
 * - 재난 유형(disasterType), 안전 여부(safety), 신뢰도(confidence) 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {
    private String disasterType;
    private String safety;
    private double confidence;
}