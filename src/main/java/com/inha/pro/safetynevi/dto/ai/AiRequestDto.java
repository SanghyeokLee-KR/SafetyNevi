package com.inha.pro.safetynevi.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 분석 요청 DTO
 * - Python 서버로 전송할 텍스트 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestDto {
    private String text;
}