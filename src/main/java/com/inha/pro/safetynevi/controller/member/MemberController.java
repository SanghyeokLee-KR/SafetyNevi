package com.inha.pro.safetynevi.controller.member;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import com.inha.pro.safetynevi.dto.map.BoardDto;
import com.inha.pro.safetynevi.dto.member.MemberSignupDto;
import com.inha.pro.safetynevi.dto.member.MemberResponse;
import com.inha.pro.safetynevi.service.inquiry.InquiryService;
import com.inha.pro.safetynevi.service.map.BoardService;
import com.inha.pro.safetynevi.service.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    @Value("${api.kakao.jsKey}")
    private String kakaoJsKey;

    private final MemberService memberService;
    private final BoardService boardService;
    private final InquiryService inquiryService;

    // 보안질문 번호 -> 질문 텍스트
    private final Map<Integer, String> questionMap = Map.of(
            1, "인생 좌우명?", 2, "보물 1호?", 3, "기억에 남는 선생님?", 4, "졸업한 초등학교?", 5, "다시 태어나면 되고싶은 것?"
    );

    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "member/signup";
    }

    @GetMapping("/findAccount")
    public String findAccountPage() {
        return "member/findAccount";
    }

    // 마이페이지 - 정보+로그+내 글+문의내역 한 번에
    @GetMapping("/myInfo")
    public String myInfoPage(Model model, @AuthenticationPrincipal User user) {
        if (user != null) {
            String userId = user.getUsername();
            MemberResponse member = memberService.getMemberResponse(userId);

            if (member != null) {
                model.addAttribute("member", member);
                model.addAttribute("questionText", questionMap.getOrDefault(member.getPwQuestion(), "질문 없음"));
                model.addAttribute("loginLogs", memberService.getAccessLogResponses(userId));
                model.addAttribute("myInquiries", inquiryService.getMyInquiries(userId));
                model.addAttribute("myBoards", boardService.getMyBoards(userId));
            }
        }
        return "member/myInfo";
    }

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<String> signupProcess(@RequestBody MemberSignupDto signupDto) {
        try {
            memberService.signup(signupDto);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Signup failed", e);
            return ResponseEntity.badRequest().body("회원가입 실패");
        }
    }

    @PostMapping("/api/myinfo/update")
    @ResponseBody
    public ResponseEntity<?> updateInfo(@RequestBody Map<String, String> req, @AuthenticationPrincipal User user) {
        try {
            memberService.updateMemberInfo(
                    user.getUsername(), req.get("nickname"), req.get("phone"), req.get("address"), req.get("detailAddress")
            );
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/myinfo/change-pw")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> req, @AuthenticationPrincipal User user) {
        try {
            memberService.changePasswordWithVerification(
                    user.getUsername(), req.get("currentPassword"), req.get("securityAnswer"), req.get("newPassword")
            );
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 탈퇴하면 세션도 같이 끊음
    @PostMapping("/api/member/withdraw")
    @ResponseBody
    public ResponseEntity<?> withdrawMember(@RequestBody Map<String, String> req,
                                            @AuthenticationPrincipal User user,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");
        try {
            memberService.withdrawMember(user.getUsername(), req.get("password"));
            new SecurityContextLogoutHandler().logout(request, response, null);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 비번찾기 1단계 - 아이디+이메일로 보안질문 가져오기
    @PostMapping("/api/find/question")
    @ResponseBody
    public ResponseEntity<?> getQuestion(@RequestBody Map<String, String> request) {
        try {
            Integer qNum = memberService.findPwQuestion(request.get("userId"), request.get("email"));
            return ResponseEntity.ok(Collections.singletonMap("question", qNum));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    // 비번찾기 2단계 - 보안질문 답 맞는지 확인
    @PostMapping("/api/find/verify")
    @ResponseBody
    public ResponseEntity<?> verifyAnswer(@RequestBody Map<String, String> request, HttpSession session) {
        String userId = request.get("userId");
        boolean isCorrect = memberService.verifyPwAnswer(userId, request.get("answer"));
        if (isCorrect) {
            // 통과한 userId를 세션에 박아둬야 다음 단계에서 본인확인 강제 가능
            session.setAttribute("PW_RESET_VERIFIED", userId);
            return ResponseEntity.ok("verified");
        }
        session.removeAttribute("PW_RESET_VERIFIED");
        return ResponseEntity.badRequest().body("Answer mismatch");
    }

    // 비번찾기 3단계 - 실제 재설정
    @PostMapping("/api/find/reset")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String userId = request.get("userId");
        // 2단계 통과한 같은 세션·같은 userId만 허용 (남의 계정 비번 못 바꾸게)
        Object verified = session.getAttribute("PW_RESET_VERIFIED");
        if (verified == null || !verified.equals(userId)) {
            return ResponseEntity.status(403).body("본인 확인이 필요합니다.");
        }
        try {
            memberService.resetPassword(userId, request.get("password"));
            session.removeAttribute("PW_RESET_VERIFIED");
            return ResponseEntity.ok("changed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Reset failed");
        }
    }
}