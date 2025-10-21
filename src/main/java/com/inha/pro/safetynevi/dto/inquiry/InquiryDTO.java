package com.inha.pro.safetynevi.dto.inquiry;

import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class InquiryDTO {

    // --- 기본 정보 ---
    private Long id;
    private String title;
    private String content;
    private String category;      // 예: 결제, 이용장애, 기타
    private String imageUrl;

    // --- 작성자 정보 ---
    private String writerId;      // 프로필 링크 등에 쓰는 고유 ID
    private String writerName;

    // --- 상태 및 설정 ---
    private String status;        // WAITING, COMPLETED 등
    private Integer isSecret;
    private String answerContent;

    // --- 시간 정보 ---
    private LocalDateTime createdDate;
    private LocalDateTime answerDate;

    // 업로드 받는 용도, DB에는 안 들어감
    private MultipartFile file;

    public static InquiryDTO toDto(InquiryEntity entity) {
        InquiryDTO dto = new InquiryDTO();

        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setCategory(entity.getCategory());
        dto.setImageUrl(entity.getImageUrl());
        dto.setWriterId(entity.getWriterId());
        dto.setWriterName(entity.getWriterName());
        dto.setStatus(entity.getStatus().toString());
        dto.setIsSecret(entity.getIsSecret());
        dto.setAnswerContent(entity.getAnswerContent());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setAnswerDate(entity.getAnswerDate());

        return dto;
    }

}
