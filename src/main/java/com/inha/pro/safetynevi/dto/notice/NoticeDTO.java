package com.inha.pro.safetynevi.dto.notice;

import com.inha.pro.safetynevi.entity.notice.NoticeEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class NoticeDTO {

    private Long id;
    private String title;
    private String content;

    // HTML select 태그의 value ("GENERAL", "IMPORTANT", "EMERGENCY")가 들어옴
    private String type;

    private int viewCount;
    private LocalDateTime createdDate;

    // --- 작성자 정보 ---
    private String writerId;
    private String writerName;

    // --- 파일 처리 ---
    private MultipartFile file; // HTML form의 <input type="file">을 받는 용도
    private String attachmentUrl; // DB에서 꺼낸 파일 경로를 보여주는 용도
    private String originalFileName; // (옵션) 원본 파일명 표시용

    // Entity -> DTO 변환 메서드
    public static NoticeDTO toDto(NoticeEntity entity) {
        NoticeDTO dto = new NoticeDTO();

        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType().toString()); // Enum -> String
        dto.setViewCount(entity.getViewCount());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setAttachmentUrl(entity.getAttachmentUrl());
        dto.setWriterId(entity.getWriterId());
        dto.setWriterName(entity.getWriterName());

        return dto;
    }

    // (옵션) 화면에 "일반 공지", "긴급" 처럼 한글로 보여주기 위한 Getter 추가
    public String getTypeDescription() {
        if (this.type == null) return "일반 공지";
        try {
            return NoticeEntity.NoticeType.valueOf(this.type).getDescription();
        } catch (IllegalArgumentException e) {
            return "일반 공지";
        }
    }
}