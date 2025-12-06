package com.inha.pro.safetynevi.service.member;

import com.inha.pro.safetynevi.dao.map.FavoritePlaceRepository;
import com.inha.pro.safetynevi.dao.member.*;
import com.inha.pro.safetynevi.dto.member.AccessLogResponse;
import com.inha.pro.safetynevi.dto.member.MemberResponse;
import com.inha.pro.safetynevi.dto.member.MemberSignupDto;
import com.inha.pro.safetynevi.entity.member.*;
import com.inha.pro.safetynevi.util.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final AccessLogRepository accessLogRepository;
    private final InquiryRepository inquiryRepository;
    private final FavoritePlaceRepository favoritePlaceRepository;
    private final FamilyRepository familyRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    // 비번찾기 시도제한 키 접두어 (로그인 잠금과 분리)
    private static final String PW_FIND_PREFIX = "pwfind:";

    public void signup(MemberSignupDto dto) {
        validatePassword(dto.getPassword());

        // 클라 검사 우회로 기존 계정(admin 포함) 가로채는 거 막으려고 서버에서도 중복 체크
        if (memberRepository.existsByUserId(dto.getUserId()))
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (dto.getEmail() != null && memberRepository.existsByEmail(dto.getEmail()))
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (dto.getNickname() != null && memberRepository.existsByNickname(dto.getNickname()))
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");

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
                .role("USER")
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

    // 비밀번호 찾기 (보안질문 기반)
    // 계정 존재 여부가 응답으로 드러나지 않게 항상 질문 번호를 돌려준다.
    // 불일치 시엔 userId로 고정된 더미 질문을 반환(요청마다 바뀌지 않게).
    // 실제 재설정은 2단계 답변 검증 + 세션 토큰으로 막혀 있어 더미 노출은 무해.
    @Transactional(readOnly = true)
    public Integer findPwQuestion(String userId, String email) {
        return memberRepository.findByUserIdAndEmail(userId, email)
                .map(Member::getPwQuestion)
                .orElseGet(() -> decoyQuestion(userId));
    }

    // userId 기반 1~5 고정 더미 질문 번호
    private Integer decoyQuestion(String userId) {
        if (userId == null || userId.isBlank()) return 1;
        return Math.floorMod(userId.toLowerCase().hashCode(), 5) + 1;
    }

    // 보안질문 답 검증. 없는 계정이어도 throw 없이 false (존재 여부 비노출).
    // 같은 userId로 너무 많이 틀리면 일정 시간 잠근다(brute-force 방어).
    @Transactional(readOnly = true)
    public boolean verifyPwAnswer(String userId, String rawAnswer) {
        String key = PW_FIND_PREFIX + userId;
        if (loginAttemptService.isBlocked(key)) {
            throw new IllegalArgumentException("잠시 후 다시 시도해주세요.");
        }
        Member member = memberRepository.findById(userId).orElse(null);
        if (member == null || !passwordEncoder.matches(rawAnswer, member.getPwAnswer())) {
            loginAttemptService.loginFailed(key);
            return false;
        }
        loginAttemptService.loginSucceeded(key);
        return true;
    }

    public void resetPassword(String userId, String newPassword) {
        validatePassword(newPassword);
        Member member = memberRepository.findById(userId).orElseThrow();
        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    // 마이페이지 정보 수정
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
    public boolean isAdmin(String userId) {
        return memberRepository.findById(userId).map(Member::isAdmin).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AccessLog> getAccessLogs(String userId) {
        return accessLogRepository.findTop20ByUserIdOrderByLogDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessLogResponses(String userId) {
        return accessLogRepository.findTop20ByUserIdOrderByLogDateDesc(userId)
                .stream().map(AccessLogResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Inquiry> getInquiries(String userId) {
        return inquiryRepository.findAllByMember_UserIdOrderByCreatedAtDesc(userId);
    }

    // 여기부터 관리자 기능
    @Transactional(readOnly = true)
    public long countMembers() { return memberRepository.count(); }

    @Transactional(readOnly = true)
    public List<Member> findAllMembers() { return memberRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<MemberResponse> findAllMemberResponses() {
        return memberRepository.findAll().stream().map(MemberResponse::from).toList();
    }

    // 관리자 회원 목록 페이징 (가입 최신순)
    @Transactional(readOnly = true)
    public Page<MemberResponse> findMemberResponses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("joinDate")));
        return memberRepository.findAll(pageable).map(MemberResponse::from);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberResponse(String userId) {
        return memberRepository.findById(userId).map(MemberResponse::from).orElse(null);
    }

    public void withdrawMember(String userId, String password) {
        Member member = memberRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(password, member.getPassword())) throw new IllegalArgumentException("비밀번호 불일치");
        deleteUserOwnedData(userId);
        memberRepository.delete(member);
    }

    public void forceWithdraw(String userId) {
        Member member = memberRepository.findById(userId).orElseThrow();
        deleteUserOwnedData(userId);
        memberRepository.delete(member);
    }

    // 게시글·댓글·좋아요·문의·신고는 FK에 걸린 ON DELETE CASCADE로 회원과 함께 지워진다.
    // 장소·가족·접속로그는 userId만 들고 있어 직접 정리.
    private void deleteUserOwnedData(String userId) {
        favoritePlaceRepository.deleteByUserId(userId);
        familyRepository.deleteByUserId(userId);
        accessLogRepository.deleteByUserId(userId);
    }
}