package com.inha.pro.safetynevi.controller.notice;

import com.inha.pro.safetynevi.dto.notice.NoticeDTO;
import com.inha.pro.safetynevi.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService nsvc;

    @GetMapping("/notice")
    public String notice(Model model,
                         @PageableDefault(page = 0, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                         @RequestParam(value = "keyword", defaultValue = "") String keyword,
                         @RequestParam(value = "limit", defaultValue = "10") int limit) {

        // 페이지당 개수(limit)는 드롭다운에서 바꾸므로 다시 만들어줌
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), limit, pageable.getSort());

        Page<NoticeDTO> pagingData = nsvc.getNoticeList(newPageable, keyword);

        // 페이지 번호를 5개씩 블록으로 끊어서 보여줌
        int blockAmount = 5;
        int startPage = (int) Math.floor((double) pagingData.getNumber() / blockAmount) * blockAmount + 1;
        int endPage = Math.min(startPage + blockAmount - 1, pagingData.getTotalPages());

        if (pagingData.getTotalPages() == 0) {
            startPage = 1;
            endPage = 1;
        }

        model.addAttribute("paging", pagingData);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("keyword", keyword); // 검색창에 입력값 유지
        model.addAttribute("selectedLimit", limit);

        return "notice/notice";
    }

    @GetMapping("/noticeDetail")
    public String noticeDetail(@RequestParam("id") Long id, Model model) {
        NoticeDTO noticeDTO = nsvc.getNoticeDetail(id);
        model.addAttribute("notice", noticeDTO);
        return "notice/noticeDetail";
    }

    @PostMapping("/admin/notice/NoticeWrite")
    public String saveNotice(@ModelAttribute NoticeDTO noticeDTO,
                             @AuthenticationPrincipal UserDetails user) {

        // TODO 관리자 권한 체크 넣기
        if (user == null) {
            return "redirect:/login";
        }

        nsvc.saveNotice(noticeDTO, user.getUsername());
        return "redirect:/admin/notice/create";
    }


}