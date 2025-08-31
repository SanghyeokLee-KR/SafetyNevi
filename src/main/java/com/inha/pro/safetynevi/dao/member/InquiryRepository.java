package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // 내 아이디(userId)로 작성된 문의글을 최신순으로 조회하는 메서드
    // (Member 엔티티의 userId 필드를 기준으로 검색)
    List<Inquiry> findAllByMember_UserIdOrderByCreatedAtDesc(String userId);
}