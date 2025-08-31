package com.inha.pro.safetynevi.service;

import com.inha.pro.safetynevi.dao.member.UserSuspensionRepository;
import com.inha.pro.safetynevi.entity.member.UserSuspension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 회원 이용 정지(Suspension) 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class SuspensionService {

    private final UserSuspensionRepository suspensionRepository;

    // 회원 정지 처리
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

    // 현재 정지 상태인지 확인
    public boolean isSuspended(String userId) {
        LocalDateTime now = LocalDateTime.now();
        // 현재 시간이 정지 기간(start ~ end) 내에 포함되는지 확인
        return suspensionRepository.existsByTargetUserIdAndStartAtLessThanEqualAndEndAtAfterOrEndAtIsNull(
                userId, now, now
        );
    }
}