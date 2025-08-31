package com.inha.pro.safetynevi.entity.report;

import com.inha.pro.safetynevi.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 신고(Report) 엔티티
 * - 시설 정보 오류, 게시글 신고, 악성 유저 신고 데이터 저장
 * - 관리자 페이지에서 상태(STATUS) 변경 가능
 */
@Entity
@Table(name = "SAFETY_REPORT")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPORT_ID")
    private Long id;

    // 신고자
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "REPORTER_ID", nullable = false)
    private Member reporter;

    // 신고 대상 유형 (FACILITY, BOARD, USER)
    @Column(name = "TARGET_TYPE", nullable = false)
    private String targetType;

    // 신고 대상 ID (PK)
    @Column(name = "TARGET_ID")
    private Long targetId;

    // 신고 대상 닉네임/아이디 (유저 차단용)
    @Column(name = "TARGET_USER")
    private String targetUser;

    // 신고 사유 코드 (abuse, spam 등)
    @Column(name = "REASON", nullable = false)
    private String reason;

    // 상세 내용
    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    // 처리 상태 (RECEIVED -> PROCESSING -> DONE)
    @Column(name = "STATUS", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    // 상태 변경 비즈니스 메서드
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
}