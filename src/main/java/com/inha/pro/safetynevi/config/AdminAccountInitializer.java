package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 권한 판별을 username 하드코딩 -> ROLE 컬럼으로 옮기면서,
// 기존 admin 계정이 권한 잃지 않게 시작할 때 ROLE 한 번 보정해줌
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
