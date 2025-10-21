package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.member.MemberResponse;
import com.inha.pro.safetynevi.dto.report.ReportResponse;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import com.inha.pro.safetynevi.service.map.BoardService;
import com.inha.pro.safetynevi.service.member.MemberService;
import com.inha.pro.safetynevi.service.report.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final MemberService memberService;
    private final BoardService boardService;
    private final DisasterService disasterService;
    private final ReportService reportService;

    // 모든 화면 공통: 사이드바 메뉴 활성화용 현재 URI
    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("memberCount", memberService.countMembers());
        model.addAttribute("boardCount", boardService.countBoards());
        model.addAttribute("disasterCount", disasterService.countDisasters());
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(Model model) {
        List<MemberResponse> members = memberService.findAllMemberResponses();
        model.addAttribute("members", members);
        return "admin/members";
    }

    @GetMapping("/boards")
    public String boards(Model model) {
        return "admin/boards";
    }

    @GetMapping("/reports")
    public String reports(Model model, @RequestParam(defaultValue = "0") int page) {
        Page<ReportResponse> reportPage = reportService.getAllReports(page, 10).map(ReportResponse::from);
        model.addAttribute("reports", reportPage);
        return "admin/reports";
    }

    @GetMapping("/disaster")
    public String disasterPage() {
        // 재난 목록은 화면 JS가 /api/disaster-zones로 따로 불러옴
        return "admin/disaster";
    }
}