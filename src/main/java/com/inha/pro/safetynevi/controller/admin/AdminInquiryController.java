package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import com.inha.pro.safetynevi.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService isvc;

    @GetMapping
    public String adminInquiryList(Model model) {

        List<InquiryDTO> unansweredList = isvc.getUnansweredInquiries();   // 미답변
        List<InquiryDTO> answeredList = isvc.getRecentAnsweredInquiries(); // 답변완료 최근 5건

        model.addAttribute("unansweredList", unansweredList);
        model.addAttribute("answeredList", answeredList);

        // 사이드바에서 현재 메뉴 활성화하려고 넘김
        model.addAttribute("requestURI", "/admin/inquiries");

        return "admin/inquiries";
    }


}
