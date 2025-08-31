package com.inha.pro.safetynevi.controller.main;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/disasterGuide")
    public String disasterGuide() {
        return "resources-templates/disasterGuide";
    }

    @GetMapping("/shelterDetail")
    public String shelterDetail() {
        return "resources-templates/shelterDetail";
    }

}

