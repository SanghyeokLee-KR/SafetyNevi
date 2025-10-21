package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.entity.member.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 로컬(h2)에서만 테스트 계정 시드. 운영 프로파일에선 안 돎
@Slf4j
@Component
@Profile("h2")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!memberRepository.existsById("admin")) {
            memberRepository.save(Member.builder()
                    .userId("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .email("admin@safetynevi.local")
                    .name("관리자")
                    .nickname("관리자")
                    .address("서울특별시")
                    .pwQuestion(1)
                    .pwAnswer(passwordEncoder.encode("test"))
                    .role("ADMIN")
                    .build());
            log.info("[dev] 테스트 관리자 계정 생성: admin / admin1234");
        }

        if (!memberRepository.existsById("test")) {
            memberRepository.save(Member.builder()
                    .userId("test")
                    .password(passwordEncoder.encode("test1234"))
                    .email("test@safetynevi.local")
                    .name("테스트")
                    .nickname("테스트유저")
                    .address("서울특별시")
                    .pwQuestion(1)
                    .pwAnswer(passwordEncoder.encode("test"))
                    .role("USER")
                    .build());
            log.info("[dev] 테스트 일반 계정 생성: test / test1234");
        }
    }
}
