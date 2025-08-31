package com.inha.pro.safetynevi.entity.inquiry;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.type.NumericBooleanConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
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

    // --- 작성자 (이름 매핑 확실하게!) ---
    @Column(name = "WRITER_ID", nullable = false) // DB의 WRITER_ID와 매핑
    private String writerId;

    @Column(name = "WRITER_NAME", nullable = false) // DB의 WRITER_NAME과 매핑
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

    // --- [비즈니스 로직 메서드] ---
    // Entity 안에 로직을 두면 객체지향적이며 관리가 편해집니다.

    // 답변 등록 기능
    public void registerAnswer(String answer) {
        this.answerContent = answer;
        this.answerDate = LocalDateTime.now();
        this.status = InquiryStatus.COMPLETED; // 답변이 달리면 상태를 완료로 변경
    }

    // 내부 Enum 정의 (혹은 별도 파일로 분리 가능)
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
                .imageUrl(dto.getImageUrl()) // 서비스에서 만들어준 URL
                .writerId(dto.getWriterId())
                .writerName(dto.getWriterName())
                .isSecret(dto.getIsSecret() != null ? dto.getIsSecret() : 0)

                // ▼▼▼ [여기를 수정하세요] ▼▼▼
                // dto.getStatus()가 null이면 기본값(WAITING)을 넣고, 값이 있으면 변환해서 넣는다.
                .status(dto.getStatus() != null && !dto.getStatus().isEmpty()
                        ? InquiryStatus.valueOf(dto.getStatus())
                        : InquiryStatus.WAITING)
                // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

                .answerContent(dto.getAnswerContent())
                // createdDate는 @CreatedDate가 알아서 하므로 생략
                .build();
    }

    // [수정 메서드] 제목, 내용, 카테고리, 비밀글 여부, 이미지 경로를 한 번에 업데이트
    public void modifyInquiry(String title, String content, String category, int secret, String imageUrl) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.isSecret = isSecret;

        // 이미지가 새로 들어왔을 때만 변경 (null이면 기존 이미지 유지)
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}