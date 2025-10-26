package com.inha.pro.safetynevi.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {

    @NotBlank(message = "신고 대상 유형은 필수입니다.")
    private String targetType; // FACILITY, BOARD

    @NotNull(message = "신고 대상 ID는 필수입니다.")
    private Long targetId;

    @Size(max = 50, message = "대상 사용자 값이 너무 깁니다.")
    private String targetUser; // 게시글 작성자 등

    @NotBlank(message = "신고 사유는 필수입니다.")
    @Size(max = 50, message = "신고 사유 값이 너무 깁니다.")
    private String reason;     // 사유 코드

    @Size(max = 1000, message = "상세 내용은 1000자 이내여야 합니다.")
    private String description;
}