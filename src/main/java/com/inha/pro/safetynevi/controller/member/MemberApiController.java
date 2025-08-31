package com.inha.pro.safetynevi.controller.member;

import com.inha.pro.safetynevi.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원 가입 시 실시간 중복 검사 API
 */
@RestController
@RequestMapping("/api/check")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // 아이디 중복 확인
    @GetMapping("/id")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam("userId") String userId) {
        boolean exists = memberService.checkUserIdDuplicate(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !exists);
        return ResponseEntity.ok(response);
    }

    // 이메일 중복 확인
    @GetMapping("/email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam("email") String email) {
        boolean exists = memberService.checkEmailDuplicate(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !exists);
        return ResponseEntity.ok(response);
    }

    // 닉네임 중복 확인
    @GetMapping("/nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam("nickname") String nickname) {
        boolean exists = memberService.checkNicknameDuplicate(nickname);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !exists);
        return ResponseEntity.ok(response);
    }
}