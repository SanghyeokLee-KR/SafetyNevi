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

/**
 * 관리자 페이지 뷰 컨트롤러 (Thymeleaf 렌더링)
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final MemberService memberService;
    private final BoardService boardService;
    private final DisasterService disasterService;
    private final ReportService reportService;

    // 공통 모델 속성: 현재 요청 URI (네비게이션 활성화용)
    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    // 1. 대시보드 메인
    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("memberCount", memberService.countMembers());
        model.addAttribute("boardCount", boardService.countBoards());
        model.addAttribute("disasterCount", disasterService.countDisasters());
        return "admin/dashboard";
    }

    // 2. 회원 관리 페이지
    @GetMapping("/members")
    public String members(Model model) {
        List<MemberResponse> members = memberService.findAllMemberResponses();
        model.addAttribute("members", members);
        return "admin/members";
    }

    // 3. 게시물 관리 페이지
    @GetMapping("/boards")
    public String boards(Model model) {
        return "admin/boards";
    }

    // 4. 신고 관리 페이지 (페이징 적용)
    @GetMapping("/reports")
    public String reports(Model model, @RequestParam(defaultValue = "0") int page) {
        Page<ReportResponse> reportPage = reportService.getAllReports(page, 10).map(ReportResponse::from);
        model.addAttribute("reports", reportPage);
        return "admin/reports";
    }

    // 5. 재난 관리 및 시뮬레이션 페이지
    @GetMapping("/disaster")
    public String disasterPage() {
        // 재난 목록은 화면에서 /api/disaster-zones(JS)로 불러오므로 모델에 담지 않는다
        return "admin/disaster";
    }
}