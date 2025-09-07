package com.inha.pro.safetynevi.dto.member;

import com.inha.pro.safetynevi.entity.member.AccessLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 접속 로그 응답 DTO
 * - 마이페이지 접속 이력에 보여줄 값만 노출한다.
 */
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
