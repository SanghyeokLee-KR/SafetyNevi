package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 접속 로그 엔티티 (로그인/로그아웃 이력)
 */
@Entity
@Table(name = "SAFETY_ACCESS_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccessLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_ID")
    private Long id;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "ACCESS_TYPE") // LOGIN, LOGOUT
    private String accessType;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "USER_AGENT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "LOG_DATE")
    private LocalDateTime logDate;
}