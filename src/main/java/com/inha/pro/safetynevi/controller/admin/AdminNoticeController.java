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

    @GetMapping("/create") // URL이 /create인데 사실상 목록 페이지, 굳어져서 그냥 둠
    public String noticeManagePage(Model model,
                                   @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // 검색 안 하니까 keyword는 null (전체 + 중요도순)
        Page<NoticeDTO> noticeList = nsvc.getNoticeList(pageable, null);

        model.addAttribute("notices", noticeList);
        model.addAttribute("requestURI", "/admin/notice/create"); // 사이드바 활성화용

        return "admin/notice-create";
    }

    @PostMapping("/delete/{id}")
    public String deleteNotice(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails user) {
        if (user == null) return "redirect:/login";

        // TODO 관리자 권한 체크 넣기
        nsvc.deleteNotice(id);

        return "redirect:/admin/notice/create";
    }
}
