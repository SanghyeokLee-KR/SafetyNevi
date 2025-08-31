package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    // 유저별 등록된 가족 연락처 목록 조회
    List<Family> findAllByUserId(String userId);
}