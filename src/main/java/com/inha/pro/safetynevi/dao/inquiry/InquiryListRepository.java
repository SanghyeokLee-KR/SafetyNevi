package com.inha.pro.safetynevi.dao.inquiry;

import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity;
import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryListRepository extends JpaRepository<InquiryEntity, Long> {

    // 미답변 목록 (관리자용)
    List<InquiryEntity> findByStatusOrderByCreatedDateDesc(InquiryStatus status);

    // 답변 완료 최근 5건 (관리자용)
    List<InquiryEntity> findTop5ByStatusOrderByAnswerDateDesc(InquiryStatus status);

    // 답변 완료 페이징 (관리자용)
    Page<InquiryEntity> findByStatusOrderByAnswerDateDesc(InquiryStatus status, Pageable pageable);

    // 내가 쓴 문의 목록
    List<InquiryEntity> findAllByWriterIdOrderByCreatedDateDesc(String writerId);
}