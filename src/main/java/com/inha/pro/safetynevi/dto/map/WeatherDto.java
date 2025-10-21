package com.inha.pro.safetynevi.dto.map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherDto {
    private String address;       // 좌표를 변환한 주소명
    private String temp;
    private String weatherStatus; // 맑음, 흐림 등
    private String weatherIcon;   // 프론트 아이콘 매핑용 파일명
}