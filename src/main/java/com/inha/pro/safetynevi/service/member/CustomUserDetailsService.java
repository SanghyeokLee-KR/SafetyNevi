package com.inha.pro.safetynevi.service.member;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.entity.member.Member;
import com.inha.pro.safetynevi.service.SuspensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 사용자 인증 서비스
 * - DB에서 사용자 정보를 조회하여 UserDetails 객체 반환
 * - 정지된 계정(Suspended) 로그인 차단 로직 포함
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final SuspensionService suspensionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 정지 계정 체크
        if (suspensionService.isSuspended(username)) {
            throw new DisabledException("Account is suspended.");
        }

        String role = "admin".equals(member.getUserId()) ? "ADMIN" : "USER";

        return User.builder()
                .username(member.getUserId())
                .password(member.getPassword())
                .roles(role)
                .build();
    }
}