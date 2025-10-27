package com.inha.pro.safetynevi.service.member;

import com.inha.pro.safetynevi.dao.member.AccessLogRepository;
import com.inha.pro.safetynevi.dao.member.InquiryRepository;
import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.dto.member.MemberSignupDto;
import com.inha.pro.safetynevi.entity.member.Member;
import com.inha.pro.safetynevi.util.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock AccessLogRepository accessLogRepository;
    @Mock InquiryRepository inquiryRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LoginAttemptService loginAttemptService;
    @InjectMocks MemberService memberService;

    @Test
    void 이미_쓰는_아이디면_가입에_실패한다() {
        MemberSignupDto dto = mock(MemberSignupDto.class);
        when(dto.getPassword()).thenReturn("Test1234!");
        when(dto.getUserId()).thenReturn("dupId");
        when(memberRepository.existsByUserId("dupId")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void role이_ADMIN이면_isAdmin은_true() {
        Member admin = Member.builder().userId("admin").role("ADMIN").build();
        when(memberRepository.findById("admin")).thenReturn(Optional.of(admin));

        assertThat(memberService.isAdmin("admin")).isTrue();
    }

    @Test
    void role이_USER면_isAdmin은_false() {
        Member user = Member.builder().userId("u").role("USER").build();
        when(memberRepository.findById("u")).thenReturn(Optional.of(user));

        assertThat(memberService.isAdmin("u")).isFalse();
    }

    @Test
    void 비번찾기_질문은_존재하는_계정의_질문번호를_반환한다() {
        Member m = Member.builder().userId("u").pwQuestion(3).build();
        when(memberRepository.findByUserIdAndEmail("u", "e@x.com")).thenReturn(Optional.of(m));

        assertThat(memberService.findPwQuestion("u", "e@x.com")).isEqualTo(3);
    }

    @Test
    void 비번찾기_질문은_없는_계정도_더미질문을_반환해_존재여부를_숨긴다() {
        when(memberRepository.findByUserIdAndEmail("ghost", "g@x.com")).thenReturn(Optional.empty());

        Integer q = memberService.findPwQuestion("ghost", "g@x.com");

        assertThat(q).isNotNull();      // null·예외로 '없는 계정'임을 드러내지 않음
        assertThat(q).isBetween(1, 5);
    }

    @Test
    void 보안질문답_검증은_없는_계정이면_예외없이_false() {
        when(loginAttemptService.isBlocked("pwfind:ghost")).thenReturn(false);
        when(memberRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThat(memberService.verifyPwAnswer("ghost", "answer")).isFalse();
        verify(loginAttemptService).loginFailed("pwfind:ghost");
    }
}
