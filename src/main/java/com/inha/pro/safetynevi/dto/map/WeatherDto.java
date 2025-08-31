package com.inha.pro.safetynevi.dto.map;

import lombok.Builder;
import lombok.Data;

/**
 * 날씨 정보 응답 DTO
 */
@Data
@Builder
public class WeatherDto {
    private String address;       // 변환된 주소명
    private String temp;          // 기온
    private String weatherStatus; // 날씨 상태 (맑음, 흐림 등)
    private String weatherIcon;   // 클라이언트 아이콘 매핑용 파일명
}