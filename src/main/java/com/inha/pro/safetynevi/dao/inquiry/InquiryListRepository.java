package com.inha.pro.safetynevi.dao.inquiry;

import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity;
import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryListRepository extends JpaRepository<InquiryEntity, Long> {

    // 1. 미답변 목록 (관리자용)
    // Entity의 'status'와 'createdDate'를 기준으로 찾습니다.
    List<InquiryEntity> findByStatusOrderByCreatedDateDesc(InquiryStatus status);

    // 2. 답변 완료 목록 (관리자용)
    // Entity의 'status'와 'answerDate'를 기준으로 찾습니다.
    List<InquiryEntity> findTop5ByStatusOrderByAnswerDateDesc(InquiryStatus status);

    // 작성자 ID(writerId)로 검색하고, 작성일(createdDate) 내림차순으로 정렬합니다.
    List<InquiryEntity> findAllByWriterIdOrderByCreatedDateDesc(String writerId);
}