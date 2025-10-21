package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_SUSPENSION")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserSuspension {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TARGET_USER_ID", nullable = false)
    private String targetUserId;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "START_AT")
    private LocalDateTime startAt;

    @Column(name = "END_AT")
    private LocalDateTime endAt; // Null일 경우 영구 정지

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}