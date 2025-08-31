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

        // 1. 미답변 목록 (WAITING)
        List<InquiryDTO> unansweredList = isvc.getUnansweredInquiries();

        // 2. 답변 완료 목록 (COMPLETED, 최근 5건)
        List<InquiryDTO> answeredList = isvc.getRecentAnsweredInquiries();

        // 3. 모델에 담기
        model.addAttribute("unansweredList", unansweredList);
        model.addAttribute("answeredList", answeredList);

        // 4. 현재 URI (사이드바 활성화용)
        model.addAttribute("requestURI", "/admin/inquiries");

        return "admin/inquiries"; // admin 폴더 안의 inquiries.html
    }


}
