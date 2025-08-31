package com.inha.pro.safetynevi.entity.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 가족/지인 안심 연락처 엔티티
 */
@Entity
@Table(name = "SAFETY_FAMILY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Family {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAMILY_ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @CreationTimestamp
    private LocalDateTime createdAt;
}