package com.inha.pro.safetynevi.controller.inquiry;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import com.inha.pro.safetynevi.service.inquiry.InquiryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inquiry")
public class InquiryController {

    private final InquiryService isvc;

    @GetMapping("/list")
    public String inquiryList(Model model,
                              @PageableDefault(page = 0, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                              @RequestParam(value = "limit", defaultValue = "10") int limit,
                              @AuthenticationPrincipal UserDetails user) {

        // 페이지당 개수(limit)를 드롭다운에서 바꾸므로 다시 만들어줌
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), limit, pageable.getSort());

        Page<InquiryDTO> pagingData = isvc.getInquiryList(newPageable);

        // 페이지 번호를 5개씩 블록으로 끊어 보여줌
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
        model.addAttribute("selectedLimit", limit);

        // 비밀글 본인 여부 판별하려고 로그인 아이디를 뷰로 넘김
        if (user != null) {
            model.addAttribute("loginUserId", user.getUsername());
        } else {
            model.addAttribute("loginUserId", "anonymous");
        }

        return "inquiry/inquiryList";
    }

    @GetMapping("/write")
    public String inquiryWriteForm() {
        return "inquiry/inquiryWrite";
    }

    @PostMapping("/write")
    public String inquiryWrite(@ModelAttribute InquiryDTO inquiryDTO,
                               @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return "redirect:/login";
        }

        isvc.writeInquiry(inquiryDTO, user.getUsername());

        return "redirect:/inquiry/list";
    }

    @GetMapping("/detail/{id}")
    public String inquiryDetail(@PathVariable("id") Long id, Model model,
                                @AuthenticationPrincipal UserDetails user) {

        String currentUserId = (user != null) ? user.getUsername() : null;

        try {
            InquiryDTO inquiryDTO = isvc.getInquiryDetail(id, currentUserId);

            model.addAttribute("inquiry", inquiryDTO);

            // 수정/삭제 버튼 노출 판단용
            if (currentUserId != null) {
                model.addAttribute("loginUserId", currentUserId);
            }

            return "inquiry/inquiryDetail";

        } catch (IllegalStateException e) {
            // 비밀글인데 권한 없으면 알림 페이지로
            model.addAttribute("msg", e.getMessage());
            model.addAttribute("url", "/inquiry/list");
            return "alert";
        }
    }

    @PostMapping("/delete/{id}")
    public String inquiryDelete(@PathVariable("id") Long id,
                                @AuthenticationPrincipal UserDetails user) {
        isvc.deleteInquiry(id, user.getUsername()); // 본인 확인은 서비스에서
        return "redirect:/inquiry/list";
    }

    @GetMapping("/modify/{id}")
    public String inquiryModifyForm(@PathVariable("id") Long id, Model model,
                                    @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return "redirect:/login";
        }

        try {
            // 본인 글인지 체크는 getInquiryDetail이 해줌
            InquiryDTO inquiryDTO = isvc.getInquiryDetail(id, user.getUsername());

            model.addAttribute("inquiry", inquiryDTO);
            return "inquiry/inquiryModify";

        } catch (IllegalStateException e) {
            // 권한 없거나 비밀글이면 목록으로
            return "redirect:/inquiry/list";
        }
    }

    @PostMapping("/modify/{id}")
    public String inquiryModify(@PathVariable("id") Long id,
                                @ModelAttribute InquiryDTO inquiryDTO,
                                @AuthenticationPrincipal UserDetails user) {

        isvc.modifyInquiry(id, inquiryDTO, user.getUsername());
        return "redirect:/inquiry/detail/" + id;
    }


    // 관리자 답변 등록
    @PostMapping("/answer/{id}")
    public String registerAnswer(@PathVariable("id") Long id,
                                 @RequestParam("answerContent") String answerContent, // HTML name이랑 맞춰야 함
                                 @AuthenticationPrincipal UserDetails user) {

        // 관리자만 답변 가능
        boolean isAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new SecurityException("관리자만 답변을 등록할 수 있습니다.");
        }

        isvc.registerAnswer(id, answerContent);
        return "redirect:/inquiry/detail/" + id;
    }
}
