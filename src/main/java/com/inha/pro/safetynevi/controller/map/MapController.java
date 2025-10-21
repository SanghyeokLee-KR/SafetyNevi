package com.inha.pro.safetynevi.controller.map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapController {

    @Value("${api.kakao.jsKey}")
    private String kakaoJsKey;

    @GetMapping("/map")
    public String showMapPage(Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "map/map";
    }
}