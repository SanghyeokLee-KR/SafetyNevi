package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    // 중복 가입 체크
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // 아이디/비번 찾기 본인확인용
    Optional<Member> findByUserIdAndEmail(String userId, String email);

    // 대시보드: 기간 내 가입자 수
    long countByJoinDateBetween(LocalDateTime start, LocalDateTime end);

    // 지역별 가입자 집계용 주소 전부 긁어옴
    @Query("SELECT m.address FROM Member m WHERE m.address IS NOT NULL")
    List<String> findAllAddresses();
}