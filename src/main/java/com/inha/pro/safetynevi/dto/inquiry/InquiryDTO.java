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
    private Long id;              // 문의 ID
    private String title;         // 제목
    private String content;       // 문의 내용
    private String category;      // 카테고리 (예: 결제, 이용장애, 기타)
    private String imageUrl;      // 첨부 이미지 URL

    // --- 작성자 정보 ---
    private String writerId;        // 작성자 고유 ID (프로필 링크 등에 사용)
    private String writerName;    // 작성자 닉네임/이름

    // --- 상태 및 설정 ---
    private String status;        // 답변 상태 (WAITING, COMPLETED 등)
    private Integer isSecret;     // 비밀글 여부

    // --- 답변 정보 (답변이 달린 경우 null이 아님) ---
    private String answerContent; // 관리자 답변 내용

    // --- 시간 정보 ---
    private LocalDateTime createdDate; // 작성일
    private LocalDateTime answerDate;  // 답변일

    // 파일 업로드를 받기 위한 필드 (DB 저장용 아님)
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
