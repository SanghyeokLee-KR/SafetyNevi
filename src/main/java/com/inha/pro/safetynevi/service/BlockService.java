package com.inha.pro.safetynevi.service;

import com.inha.pro.safetynevi.dao.member.BlockedUserRepository;
import com.inha.pro.safetynevi.entity.member.BlockedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 차단(Block) 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;

    // 사용자 차단 (중복 방지)
    public void blockUser(String userId, String blockedUser) {
        if (blockedUserRepository.existsByUserIdAndBlockedUserId(userId, blockedUser)) return;

        BlockedUser entity = BlockedUser.builder()
                .userId(userId)
                .blockedUserId(blockedUser)
                .createdAt(LocalDateTime.now())
                .build();

        blockedUserRepository.save(entity);
    }

    // 차단 목록 조회
    public List<BlockedUser> getBlockedUsers(String userId) {
        return blockedUserRepository.findAllByUserId(userId);
    }
}