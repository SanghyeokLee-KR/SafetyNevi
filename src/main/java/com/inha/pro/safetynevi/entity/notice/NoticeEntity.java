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

    @Column(nullable = false, length = 4000) // 공지 내용은 길 수 있으므로 넉넉하게
    private String content;

    // --- 중요도 설정 (일반, 중요, 긴급) ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    // --- 메타 데이터 ---
    @Column(name = "VIEW_COUNT", nullable = false)
    @Builder.Default // 빌더 사용 시 기본값 0 적용
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    // --- 첨부파일 및 작성자 ---
    @Column(name = "ATTACHMENT_URL")
    private String attachmentUrl; // 파일 경로 저장

    @Column(name = "WRITER_ID", nullable = false)
    private String writerId; // 작성자 ID (보통 admin)

    @Column(name = "WRITER_NAME", nullable = false)
    private String writerName; // 작성자 이름 (관리자)

    // --- 비즈니스 로직 ---

    // 1. 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }

    // 2. 공지 수정 (제목, 내용, 타입, 첨부파일)
    public void modifyNotice(String title, String content, NoticeType type, String attachmentUrl) {
        this.title = title;
        this.content = content;
        this.type = type;

        // 첨부파일이 변경된 경우에만 업데이트
        if (attachmentUrl != null) {
            this.attachmentUrl = attachmentUrl;
        }
    }

    // 3. 중요도 Enum 정의
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

    // 4. DTO -> Entity 변환 메서드
    public static NoticeEntity toEntity(NoticeDTO dto) {
        return NoticeEntity.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                // String으로 들어온 type을 Enum으로 변환
                .type(NoticeType.valueOf(dto.getType()))
                .writerId(dto.getWriterId())
                .writerName(dto.getWriterName())
                .attachmentUrl(dto.getAttachmentUrl()) // Service에서 저장 후 경로 주입
                // viewCount, createdDate는 자동 설정
                .build();
    }
}
