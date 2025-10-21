package com.inha.pro.safetynevi.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Python AI 서버로 보낼 분석 대상 텍스트
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestDto {
    private String text;
}