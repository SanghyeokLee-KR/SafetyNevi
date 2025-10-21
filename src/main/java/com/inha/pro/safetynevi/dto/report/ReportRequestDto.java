package com.inha.pro.safetynevi.dto.report;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {
    private String targetType; // FACILITY, BOARD
    private Long targetId;
    private String targetUser; // 게시글 작성자 등
    private String reason;     // 사유 코드
    private String description;
}