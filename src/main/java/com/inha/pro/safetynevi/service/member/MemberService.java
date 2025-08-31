package com.inha.pro.safetynevi.service.member;

import com.inha.pro.safetynevi.dao.member.*;
import com.inha.pro.safetynevi.dto.member.MemberSignupDto;
import com.inha.pro.safetynevi.entity.member.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원 관리 서비스
 * - 회원가입, 정보 수정, 탈퇴, 비밀번호 재설정
 * - 관리자용 통계 데이터 조회 지원
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final AccessLogRepository accessLogRepository;
    private final InquiryRepository inquiryRepository;
    private final PasswordEncoder passwordEncoder;

    // --- 회원 가입 ---
    public void signup(MemberSignupDto dto) {
        validatePassword(dto.getPassword());

        Member member = Member.builder()
                .userId(dto.getUserId())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .address(dto.getAddress())
                .detailAddress(dto.getDetailAddress())
                .areaName(dto.getAreaName())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .emergencyPhone(dto.getEmergencyPhone())
                .pwQuestion(dto.getPwQuestion())
                .pwAnswer(passwordEncoder.encode(dto.getPwAnswer()))
                .build();
        memberRepository.save(member);
    }

    private void validatePassword(String pw) {
        if (pw == null || pw.length() < 8) throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        int strength = 0;
        if (pw.matches(".*[A-Za-z].*")) strength++;
        if (pw.matches(".*[0-9].*")) strength++;
        if (pw.matches(".*[^A-Za-z0-9].*")) strength++;
        if (strength < 2) throw new IllegalArgumentException("영문/숫자/특수문자 중 2가지 이상 포함 필요");
    }

    @Transactional(readOnly = true)
    public boolean checkUserIdDuplicate(String userId) { return memberRepository.existsByUserId(userId); }
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) { return memberRepository.existsByEmail(email); }
    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname) { return memberRepository.existsByNickname(nickname); }

    // --- 비밀번호 찾기 ---
    @Transactional(readOnly = true)
    public Integer findPwQuestion(String userId, String email) {
        Member member = memberRepository.findByUserIdAndEmail(userId, email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return member.getPwQuestion();
    }

    @Transactional(readOnly = true)
    public boolean verifyPwAnswer(String userId, String rawAnswer) {
        Member member = memberRepository.findById(userId).orElseThrow();
        return passwordEncoder.matches(rawAnswer, member.getPwAnswer());
    }

    public void resetPassword(String userId, String newPassword) {
        validatePassword(newPassword);
        Member member = memberRepository.findById(userId).orElseThrow();
        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    // --- 마이페이지 ---
    public void updateMemberInfo(String userId, String nickname, String phone, String address, String detailAddress) {
        Member member = memberRepository.findById(userId).orElseThrow();
        member.updateInfo(nickname, phone, address, detailAddress);
    }

    public void changePasswordWithVerification(String userId, String currentPw, String securityAnswer, String newPw) {
        Member member = memberRepository.findById(userId).orElseThrow();

        if (!passwordEncoder.matches(currentPw, member.getPassword())) throw new IllegalArgumentException("현재 비밀번호 불일치");
        if (!passwordEncoder.matches(securityAnswer, member.getPwAnswer())) throw new IllegalArgumentException("보안 질문 답변 불일치");

        validatePassword(newPw);
        member.updatePassword(passwordEncoder.encode(newPw));
    }

    @Transactional(readOnly = true)
    public Member getMember(String userId) { return memberRepository.findById(userId).orElse(null); }

    @Transactional(readOnly = true)
    public List<AccessLog> getAccessLogs(String userId) {
        return accessLogRepository.findTop20ByUserIdOrderByLogDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Inquiry> getInquiries(String userId) {
        return inquiryRepository.findAllByMember_UserIdOrderByCreatedAtDesc(userId);
    }

    // --- 관리자 기능 ---
    @Transactional(readOnly = true)
    public long countMembers() { return memberRepository.count(); }

    @Transactional(readOnly = true)
    public List<Member> findAllMembers() { return memberRepository.findAll(); }

    // 탈퇴 처리
    public void withdrawMember(String userId, String password) {
        Member member = memberRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(password, member.getPassword())) throw new IllegalArgumentException("비밀번호 불일치");
        memberRepository.delete(member);
    }

    public void forceWithdraw(String userId) {
        Member member = memberRepository.findById(userId).orElseThrow();
        memberRepository.delete(member);
    }
}