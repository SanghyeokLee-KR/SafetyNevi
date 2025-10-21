package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    // 지금 기준 살아있는 정지가 있나 (endAt null이면 무기한)
    boolean existsByTargetUserIdAndStartAtLessThanEqualAndEndAtAfterOrEndAtIsNull(
            String userId,
            LocalDateTime now1,
            LocalDateTime now2
    );

    // 제일 최근 정지 1건
    UserSuspension findTop1ByTargetUserIdOrderByStartAtDesc(String userId);
}