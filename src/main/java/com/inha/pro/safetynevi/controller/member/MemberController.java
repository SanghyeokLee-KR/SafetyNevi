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

/**
 * 회원 관련 페이지 및 API 컨트롤러
 * - 로그인/가입/찾기 뷰 렌더링
 * - 회원가입, 정보수정, 탈퇴 등 비즈니스 로직 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    @Value("${api.kakao.jsKey}")
    private String kakaoJsKey;

    private final MemberService memberService;
    private final BoardService boardService;
    private final InquiryService inquiryService;

    // 비밀번호 찾기 질문 매핑
    private final Map<Integer, String> questionMap = Map.of(
            1, "인생 좌우명?", 2, "보물 1호?", 3, "기억에 남는 선생님?", 4, "졸업한 초등학교?", 5, "다시 태어나면 되고싶은 것?"
    );

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    // 회원가입 페이지
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "member/signup";
    }

    // 아이디/비밀번호 찾기 페이지
    @GetMapping("/findAccount")
    public String findAccountPage() {
        return "member/findAccount";
    }

    // 마이페이지 (정보, 로그, 작성글, 문의내역 조회)
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

    // 회원가입 요청 처리
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

    // 내 정보 수정
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

    // 비밀번호 변경 (검증 포함)
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

    // 회원 탈퇴 및 로그아웃 처리
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

    // 비밀번호 찾기: 질문 조회
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

    // 비밀번호 찾기: 답변 검증
    @PostMapping("/api/find/verify")
    @ResponseBody
    public ResponseEntity<?> verifyAnswer(@RequestBody Map<String, String> request, HttpSession session) {
        String userId = request.get("userId");
        boolean isCorrect = memberService.verifyPwAnswer(userId, request.get("answer"));
        if (isCorrect) {
            // 검증 통과한 userId를 세션에 묶어, 재설정 단계에서 본인확인을 강제한다
            session.setAttribute("PW_RESET_VERIFIED", userId);
            return ResponseEntity.ok("verified");
        }
        session.removeAttribute("PW_RESET_VERIFIED");
        return ResponseEntity.badRequest().body("Answer mismatch");
    }

    // 비밀번호 재설정
    @PostMapping("/api/find/reset")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String userId = request.get("userId");
        // 보안 질문 검증을 통과한 동일 세션·동일 userId만 재설정 허용 (계정 탈취 방지)
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