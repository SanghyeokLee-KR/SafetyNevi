package com.inha.pro.safetynevi.controller.map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 지도 화면(View) 컨트롤러
 */
@Controller
public class MapController {

    @Value("${api.kakao.jsKey}")
    private String kakaoJsKey;

    // 지도 메인 페이지 렌더링
    @GetMapping("/map")
    public String showMapPage(Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "map/map";
    }
}