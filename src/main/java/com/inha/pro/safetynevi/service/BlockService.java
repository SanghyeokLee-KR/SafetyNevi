package com.inha.pro.safetynevi.service;

import com.inha.pro.safetynevi.dao.member.BlockedUserRepository;
import com.inha.pro.safetynevi.entity.member.BlockedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;

    public void blockUser(String userId, String blockedUser) {
        // 이미 차단했으면 그냥 무시
        if (blockedUserRepository.existsByUserIdAndBlockedUserId(userId, blockedUser)) return;

        BlockedUser entity = BlockedUser.builder()
                .userId(userId)
                .blockedUserId(blockedUser)
                .createdAt(LocalDateTime.now())
                .build();

        blockedUserRepository.save(entity);
    }

    public List<BlockedUser> getBlockedUsers(String userId) {
        return blockedUserRepository.findAllByUserId(userId);
    }
}