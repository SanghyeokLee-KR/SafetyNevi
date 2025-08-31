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

    // 1. 공지사항 목록 페이지 (페이징 + 검색)
    @GetMapping("/notice")
    public String notice(Model model,
                         // 기본 정렬: 최신순 (id DESC)
                         @PageableDefault(page = 0, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                         @RequestParam(value = "keyword", defaultValue = "") String keyword,
                         @RequestParam(value = "limit", defaultValue = "10") int limit) {

        // 1. 페이지 요청 재설정 (Limit 반영)
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), limit, pageable.getSort());

        // 2. 서비스 호출 (검색어 포함)
        Page<NoticeDTO> pagingData = nsvc.getNoticeList(newPageable, keyword);

        // 3. 페이지네이션 UI 계산 (블록 단위)
        int blockAmount = 5;
        int startPage = (int) Math.floor((double) pagingData.getNumber() / blockAmount) * blockAmount + 1;
        int endPage = Math.min(startPage + blockAmount - 1, pagingData.getTotalPages());

        if (pagingData.getTotalPages() == 0) {
            startPage = 1;
            endPage = 1;
        }

        // 4. 모델에 담기
        model.addAttribute("paging", pagingData);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("keyword", keyword); // 검색어 유지
        model.addAttribute("selectedLimit", limit);

        return "notice/notice";
    }

    // 2. 공지사항 상세 내용
    @GetMapping("/noticeDetail")
    public String noticeDetail(@RequestParam("id") Long id, Model model) {

        // 서비스 호출 (조회수 증가 + 데이터 가져오기)
        NoticeDTO noticeDTO = nsvc.getNoticeDetail(id);

        model.addAttribute("notice", noticeDTO);

        return "notice/noticeDetail";
    }

    // 3. 공지사항 저장 (Form Action)
    @PostMapping("/admin/notice/NoticeWrite")
    public String saveNotice(@ModelAttribute NoticeDTO noticeDTO,
                             @AuthenticationPrincipal UserDetails user) {

        // 보안: 관리자 권한 체크 (필요시 추가)
        if (user == null) {
            return "redirect:/login";
        }

        // 서비스 호출 (DTO + 작성자 ID)
        nsvc.saveNotice(noticeDTO, user.getUsername());

        // 저장 후 공지사항 목록 페이지로 이동 (목록 페이지 URL에 맞게 수정하세요)
        return "redirect:/admin/notice/create";
    }


}