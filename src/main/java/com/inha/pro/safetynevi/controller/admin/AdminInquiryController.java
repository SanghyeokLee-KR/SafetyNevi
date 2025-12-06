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
    public String adminInquiryList(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("unansweredList", isvc.getUnansweredInquiries());     // 미답변(전체)
        model.addAttribute("answeredPage", isvc.getAnsweredInquiries(page, 10)); // 답변완료(페이징)
        return "admin/inquiries";
    }


}
