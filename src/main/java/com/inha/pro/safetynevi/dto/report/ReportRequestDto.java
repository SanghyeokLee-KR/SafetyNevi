package com.inha.pro.safetynevi.dto.report;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {
    // 신고 유형 (FACILITY, BOARD)
    private String targetType;

    // 대상 ID
    private Long targetId;
    
    // 대상 유저 (게시글 작성자 등)
    private String targetUser;
    
    // 신고 사유 코드
    private String reason;
    
    // 상세 설명
    private String description;
}