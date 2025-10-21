package com.inha.pro.safetynevi.entity.notice;

import com.inha.pro.safetynevi.dto.notice.NoticeDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SAFETY_NOTICE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_ID")
    private Long id;

    // --- 공지 기본 정보 ---
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    // 일반/중요/긴급
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    @Column(name = "VIEW_COUNT", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    // --- 첨부파일 및 작성자 ---
    @Column(name = "ATTACHMENT_URL")
    private String attachmentUrl;

    @Column(name = "WRITER_ID", nullable = false)
    private String writerId; // 보통 admin

    @Column(name = "WRITER_NAME", nullable = false)
    private String writerName;

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void modifyNotice(String title, String content, NoticeType type, String attachmentUrl) {
        this.title = title;
        this.content = content;
        this.type = type;

        // 첨부파일 바뀐 경우에만
        if (attachmentUrl != null) {
            this.attachmentUrl = attachmentUrl;
        }
    }

    public enum NoticeType {
        GENERAL("일반 공지"),
        IMPORTANT("⭐️ 중요"),
        EMERGENCY("🚨 긴급");

        private final String description;

        NoticeType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static NoticeEntity toEntity(NoticeDTO dto) {
        return NoticeEntity.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .type(NoticeType.valueOf(dto.getType())) // String -> Enum
                .writerId(dto.getWriterId())
                .writerName(dto.getWriterName())
                .attachmentUrl(dto.getAttachmentUrl())
                .build();
    }
}
