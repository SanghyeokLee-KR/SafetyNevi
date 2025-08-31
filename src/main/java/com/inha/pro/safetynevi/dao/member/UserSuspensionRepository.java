package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * 회원 이용 정지(Suspension) 이력 관리
 */
public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    // 현재 시점(now) 기준 유효한 정지 내역이 있는지 확인
    boolean existsByTargetUserIdAndStartAtLessThanEqualAndEndAtAfterOrEndAtIsNull(
            String userId,
            LocalDateTime now1,
            LocalDateTime now2
    );

    // 가장 최근의 정지 내역 조회
    UserSuspension findTop1ByTargetUserIdOrderByStartAtDesc(String userId);
}