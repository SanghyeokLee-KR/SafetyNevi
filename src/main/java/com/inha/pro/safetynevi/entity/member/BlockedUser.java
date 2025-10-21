package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_BLOCK")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BlockedUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private String userId; // 차단한 쪽

    @Column(name = "BLOCKED_USER_ID", nullable = false)
    private String blockedUserId; // 차단당한 쪽

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}