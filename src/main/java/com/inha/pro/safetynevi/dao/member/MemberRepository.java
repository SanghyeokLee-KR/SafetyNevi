package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 회원 데이터 접근 레이어 (DAO)
 */
public interface MemberRepository extends JpaRepository<Member, String> {

    // 중복 가입 체크
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // 아이디/비밀번호 찾기 인증용 조회
    Optional<Member> findByUserIdAndEmail(String userId, String email);

    // 특정 기간 내 가입 회원 수 카운트 (대시보드 통계용)
    long countByJoinDateBetween(LocalDateTime start, LocalDateTime end);

    // 지역별 가입자 통계를 위한 전체 주소 목록 조회
    @Query("SELECT m.address FROM Member m WHERE m.address IS NOT NULL")
    List<String> findAllAddresses();
}