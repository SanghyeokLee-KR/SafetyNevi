package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 사용자 차단(Block) 정보 엔티티
 */
@Entity
@Table(name = "USER_BLOCK")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BlockedUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private String userId; // 주체 (차단한 사람)

    @Column(name = "BLOCKED_USER_ID", nullable = false)
    private String blockedUserId; // 대상 (차단당한 사람)

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}