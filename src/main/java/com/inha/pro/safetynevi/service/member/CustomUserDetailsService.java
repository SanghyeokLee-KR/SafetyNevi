package com.inha.pro.safetynevi.service.member;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.entity.member.Member;
import com.inha.pro.safetynevi.service.SuspensionService;
import com.inha.pro.safetynevi.util.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final SuspensionService suspensionService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (loginAttemptService.isBlocked(username)) {
            throw new LockedException("로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        Member member = memberRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 정지 계정 체크
        if (suspensionService.isSuspended(username)) {
            throw new DisabledException("Account is suspended.");
        }

        // role 컬럼 기반으로 권한 부여 (값이 없으면 USER로 취급)
        String role = (member.getRole() != null && !member.getRole().isBlank()) ? member.getRole() : "USER";

        return User.builder()
                .username(member.getUserId())
                .password(member.getPassword())
                .roles(role)
                .build();
    }
}