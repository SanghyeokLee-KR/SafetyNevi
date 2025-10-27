package com.inha.pro.safetynevi.entity.inquiry;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.type.NumericBooleanConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "SAFETY_INQUIRY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InquiryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INQUIRY_ID")
    private Long id;

    // --- 제목, 내용 ---
    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Column(name = "CONTENT", nullable = false, length = 2000)
    private String content;

    @Column(name = "CATEGORY", nullable = false)
    private String category;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    // --- 작성자 ---
    @Column(name = "WRITER_ID", nullable = false)
    private String writerId;

    @Column(name = "WRITER_NAME", nullable = false)
    private String writerName;

    // --- 상태 ---
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.WAITING;

    @Column(name = "IS_SECRET", nullable = false)
    @Builder.Default
    private Integer isSecret = 0;

    // --- 답변 ---
    @Column(name = "ANSWER_CONTENT", length = 2000)
    private String answerContent;

    @Column(name = "ANSWER_DATE")
    private LocalDateTime answerDate;

    // --- 생성일 ---
    @CreationTimestamp
    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    public void registerAnswer(String answer) {
        this.answerContent = answer;
        this.answerDate = LocalDateTime.now();
        this.status = InquiryStatus.COMPLETED; // 답변 달리면 완료 처리
    }

    public enum InquiryStatus {
        WAITING,    // 답변 대기
        IN_PROGRESS,// 처리 중
        COMPLETED   // 답변 완료
    }

    public static InquiryEntity toEntity(InquiryDTO dto) {
        return InquiryEntity.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .writerId(dto.getWriterId())
                .writerName(dto.getWriterName())
                .isSecret(dto.getIsSecret() != null ? dto.getIsSecret() : 0)
                // 신규 문의는 항상 대기·답변없음으로 강제 (사용자가 폼에 status·answer 끼워 답변 위조하는 거 방지)
                .status(InquiryStatus.WAITING)
                .answerContent(null)
                .build();
    }

    public void modifyInquiry(String title, String content, String category, int secret, String imageUrl) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.isSecret = secret;

        // 새 이미지가 있을 때만 교체, null이면 기존 유지
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}