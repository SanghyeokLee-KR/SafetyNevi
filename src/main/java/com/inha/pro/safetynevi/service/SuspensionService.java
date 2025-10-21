package com.inha.pro.safetynevi.service;

import com.inha.pro.safetynevi.dao.member.UserSuspensionRepository;
import com.inha.pro.safetynevi.entity.member.UserSuspension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SuspensionService {

    private final UserSuspensionRepository suspensionRepository;

    public void suspendUser(String adminId, String targetUser, String reason, LocalDateTime endAt) {
        UserSuspension suspension = UserSuspension.builder()
                .targetUserId(targetUser)
                .reason(reason)
                .startAt(LocalDateTime.now())
                .endAt(endAt) // null인 경우 영구 정지로 간주
                .createdBy(adminId)
                .createdAt(LocalDateTime.now())
                .build();

        suspensionRepository.save(suspension);
    }

    public boolean isSuspended(String userId) {
        LocalDateTime now = LocalDateTime.now();
        // 지금 시각이 정지기간(start~end) 안이거나, endAt이 null(영구정지)이면 정지중
        return suspensionRepository.existsByTargetUserIdAndStartAtLessThanEqualAndEndAtAfterOrEndAtIsNull(
                userId, now, now
        );
    }
}