package com.inha.pro.safetynevi.dto.report;

import com.inha.pro.safetynevi.entity.report.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 신고자는 닉네임만 노출
@Getter
@Builder
public class ReportResponse {

    private final Long id;
    private final String targetType;
    private final Long targetId;
    private final String targetUser;
    private final String reason;
    private final String status;
    private final LocalDateTime createdAt;
    private final String reporterNickname;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetUser(report.getTargetUser())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reporterNickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .build();
    }
}
