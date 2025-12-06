package com.inha.pro.safetynevi.dto.map;

import java.time.LocalDateTime;

// 관리자 게시물 목록 한 행. open-in-view=false라 지연로딩을 못 쓰므로 트랜잭션 안에서 미리 평탄화한다.
public record AdminBoardRow(
        Long id,
        String category,
        String title,
        String writer,
        LocalDateTime createdAt
) {
}
