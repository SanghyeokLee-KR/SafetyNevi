package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부트스트랩 관리자 계정 초기화
 * - 권한 판별을 username 하드코딩에서 ROLE 컬럼으로 옮기면서, 기존 'admin' 계정이
 *   권한을 잃지 않도록 시작 시 한 번 ROLE을 ADMIN으로 보정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void run(String... args) {
        memberRepository.findById("admin").ifPresent(admin -> {
            if (!admin.isAdmin()) {
                admin.updateRole("ADMIN");
                memberRepository.save(admin);
                log.info("[init] admin 계정 ROLE을 ADMIN으로 설정했습니다.");
            }
        });
    }
}
