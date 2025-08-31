package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.notice.NoticeDTO;
import com.inha.pro.safetynevi.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notice")
public class AdminNoticeController {

    private final NoticeService nsvc;

    // 1. admin 페이지에서 공지 리스트 출력
    @GetMapping("/create") // URL이 /admin/notice/create로 되어있어서 유지함 (보통은 /list가 더 적절하긴 함)
    public String noticeManagePage(Model model,
                                   @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // 검색어는 없으므로 null 전달 (전체 조회 + 중요도 정렬)
        Page<NoticeDTO> noticeList = nsvc.getNoticeList(pageable, null);

        model.addAttribute("notices", noticeList);
        model.addAttribute("requestURI", "/admin/notice/create"); // 사이드바 활성화용

        return "admin/notice-create";
    }

    // 2. 공지사항 삭제
    @PostMapping("/delete/{id}")
    public String deleteNotice(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails user) {
        if (user == null) return "redirect:/login";

        // 관리자 권한 체크 필요 시 추가
        nsvc.deleteNotice(id);

        return "redirect:/admin/notice/create";
    }
}
