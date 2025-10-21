package com.inha.pro.safetynevi.entity.report;

import com.inha.pro.safetynevi.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// 시설 오류/게시글/유저 신고를 한 테이블에 담는다. 상태는 관리자가 바꾼다
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member reporter;

    // 신고 대상 유형 (FACILITY, BOARD, USER)
    @Column(name = "TARGET_TYPE", nullable = false)
    private String targetType;

    // 신고 대상의 PK
    @Column(name = "TARGET_ID")
    private Long targetId;

    // 유저 차단용으로 닉네임/아이디를 따로 보관
    @Column(name = "TARGET_USER")
    private String targetUser;

    // abuse, spam 같은 사유 코드
    @Column(name = "REASON", nullable = false)
    private String reason;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    // RECEIVED -> PROCESSING -> DONE
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