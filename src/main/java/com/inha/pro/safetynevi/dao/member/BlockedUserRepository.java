package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    // 특정 유저의 차단 목록 전체 조회
    List<BlockedUser> findAllByUserId(String userId);

    // 차단 여부 확인
    boolean existsByUserIdAndBlockedUserId(String userId, String blockedUserId);
}