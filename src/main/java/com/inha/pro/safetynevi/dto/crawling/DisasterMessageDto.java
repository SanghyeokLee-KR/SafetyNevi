package com.inha.pro.safetynevi.dto.crawling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동으로 만들어줍니다.
@ToString // 객체 정보를 보기 좋게 출력하기 위해 추가합니다.
public class DisasterMessageDto {

    private String disasterType; // 재난 상황 (예: 호우경보)
    private String area; // 지역
    private String sentDate; // 날짜
    private String content; // 내용
}
