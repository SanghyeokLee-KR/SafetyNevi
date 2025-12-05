package com.inha.pro.safetynevi.dto.crawling;

// 지도 사이드바 실시간 피드 응답용. id(dmid)는 무한 스크롤 커서 + 중복 방지 키로 쓴다.
public record FeedMessageDto(
        Long id,
        String disasterType,
        String area,
        String sentDate,
        String content
) {
}
