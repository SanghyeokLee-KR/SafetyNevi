package com.inha.pro.safetynevi.dto.member;

import com.inha.pro.safetynevi.entity.member.AccessLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 접속 로그 응답 DTO */
@Getter
@Builder
public class AccessLogResponse {

    private final String accessType;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime logDate;

    public static AccessLogResponse from(AccessLog log) {
        return AccessLogResponse.builder()
                .accessType(log.getAccessType())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .logDate(log.getLogDate())
                .build();
    }
}
