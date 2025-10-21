package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // 내가 쓴 문의 최신순
    List<Inquiry> findAllByMember_UserIdOrderByCreatedAtDesc(String userId);
}