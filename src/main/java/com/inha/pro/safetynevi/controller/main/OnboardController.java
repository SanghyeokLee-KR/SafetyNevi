package com.inha.pro.safetynevi.controller.main;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// QR 스캔으로 들어오는 '설치 없는' 재난 알림 구독 페이지. 로그인 불필요(공개).
// region(시/도)은 푸시 구독에 그대로 쓰이고, label은 시설명 등 표시용.
@Controller
public class OnboardController {

    @GetMapping("/onboard")
    public String onboard(@RequestParam(defaultValue = "전국") String region,
                          @RequestParam(required = false) String label,
                          Model model) {
        model.addAttribute("region", region);
        model.addAttribute("label", label);
        return "onboard";
    }
}
