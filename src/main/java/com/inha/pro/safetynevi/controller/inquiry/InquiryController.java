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

    // 1. 문의하기 리스트 (검색 기능 제거, 페이징 기능은 유지)
    @GetMapping("/list")
    public String inquiryList(Model model,
                              // id를 기준으로 내림차순(최신순) 정렬, 기본 0페이지
                              @PageableDefault(page = 0, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                              @RequestParam(value = "limit", defaultValue = "10") int limit,
                              @AuthenticationPrincipal UserDetails user) {

        // 1. 페이지 요청 정보 재설정 (사용자가 limit을 바꿀 경우를 대비)
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), limit, pageable.getSort());

        // 2. 서비스 호출 (검색어 없이 페이징 정보만 전달)
        Page<InquiryDTO> pagingData = isvc.getInquiryList(newPageable);

        // 3. 페이지네이션 UI 계산 (기존 로직 유지)
        int blockAmount = 5; // 화면 하단에 보여줄 페이지 번호 개수 (1, 2, 3, 4, 5)

        // 현재 페이지가 속한 블록의 시작 페이지 계산
        int startPage = (int) Math.floor((double) pagingData.getNumber() / blockAmount) * blockAmount + 1;
        // (참고: HTML에서 페이지 번호를 1부터 시작하게 처리하려면 +1을 하거나,
        // 뷰에서 loop.index + 1 처리를 해야 합니다. 기존 로직에 맞췄습니다.)

        // 블록의 마지막 페이지 계산
        int endPage = Math.min(startPage + blockAmount - 1, pagingData.getTotalPages());

        // 데이터가 아예 없을 경우 에러 방지
        if (pagingData.getTotalPages() == 0) {
            startPage = 1;
            endPage = 1;
        }

        // 4. Model에 데이터 담기
        model.addAttribute("paging", pagingData);    // 목록 데이터
        model.addAttribute("startPage", startPage);  // 시작 페이지 번호
        model.addAttribute("endPage", endPage);      // 끝 페이지 번호
        model.addAttribute("selectedLimit", limit);  // 몇 개씩 보기 유지

        // 로그인 아이디를 HTML로 보냄 (비밀글 판별용)
        if (user != null) {
            model.addAttribute("loginUserId", user.getUsername());
        } else {
            model.addAttribute("loginUserId", "anonymous"); // 비로그인
        }

        // 5. 뷰 반환 (inquiry 폴더 안의 list.html)
        return "inquiry/inquiryList";
    }

    // 2. 글쓰기 페이지 이동
    @GetMapping("/write")
    public String inquiryWriteForm() {
        return "inquiry/inquiryWrite";
    }

    // 3. 글쓰기 처리 (DB 저장 및 파일 업로드)
    @PostMapping("/write")
    public String inquiryWrite(@ModelAttribute InquiryDTO inquiryDTO,
                               @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            // 만약 로그인이 풀렸는데 글쓰기를 시도하면 로그인 창으로 튕기게 처리
            return "redirect:/login";
        }

        isvc.writeInquiry(inquiryDTO, user.getUsername());

        return "redirect:/inquiry/list";
    }

    // 4. 상세보기 페이지 매핑
    @GetMapping("/detail/{id}")
    public String inquiryDetail(@PathVariable("id") Long id, Model model,
                                @AuthenticationPrincipal UserDetails user) {

        // 1. 현재 접속한 사람의 ID 추출 (비회원이면 null)
        String currentUserId = (user != null) ? user.getUsername() : null;

        try {
            InquiryDTO inquiryDTO = isvc.getInquiryDetail(id, currentUserId);

            model.addAttribute("inquiry", inquiryDTO);

            // 로그인한 경우, 아이디를 모델에 담아 보냄 (HTML에서 수정/삭제 버튼 보여주기용)
            if (currentUserId != null) {
                model.addAttribute("loginUserId", currentUserId);
            }

            return "inquiry/inquiryDetail";

        } catch (IllegalStateException e) {
            // 비밀글 접근 거부 -> 알림 페이지로
            model.addAttribute("msg", e.getMessage());
            model.addAttribute("url", "/inquiry/list");
            return "alert";
        }
    }

    // 5. 삭제 기능 (버튼 누르면 동작)
    @PostMapping("/delete/{id}")
    public String inquiryDelete(@PathVariable("id") Long id,
                                @AuthenticationPrincipal UserDetails user) {
        // 서비스에서 "본인 확인" 후 삭제 처리
        isvc.deleteInquiry(id, user.getUsername());

        return "redirect:/inquiry/list";
    }

    // 6. 수정 페이지로 이동
    @GetMapping("/modify/{id}")
    public String inquiryModifyForm(@PathVariable("id") Long id, Model model,
                                    @AuthenticationPrincipal UserDetails user) {

        // [보안 1] 로그인이 풀렸는데 수정하러 들어오면 로그인 창으로
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // 본인 확인은 getInquiryDetail 내부에서 처리
            InquiryDTO inquiryDTO = isvc.getInquiryDetail(id, user.getUsername());

            model.addAttribute("inquiry", inquiryDTO);
            return "inquiry/inquiryModify";

        } catch (IllegalStateException e) {
            // 권한이 없거나 비밀글 접근 불가 등의 에러 발생 시 목록으로
            return "redirect:/inquiry/list";
        }
    }

    // 7. 수정 처리 (DB 업데이트)
    @PostMapping("/modify/{id}")
    public String inquiryModify(@PathVariable("id") Long id,
                                @ModelAttribute InquiryDTO inquiryDTO,
                                @AuthenticationPrincipal UserDetails user) {

        // 서비스로 넘겨서 수정 진행
        isvc.modifyInquiry(id, inquiryDTO, user.getUsername());

        // 수정 후 상세 페이지로 다시 이동해서 확인
        return "redirect:/inquiry/detail/" + id;
    }


    // [관리자] 답변 등록 처리
    @PostMapping("/answer/{id}")
    public String registerAnswer(@PathVariable("id") Long id,
                                 @RequestParam("answerContent") String answerContent, // HTML name 속성과 일치해야 함
                                 @AuthenticationPrincipal UserDetails user) {

        // 보안 검사: 관리자(ADMIN) 권한 확인
        boolean isAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new SecurityException("관리자만 답변을 등록할 수 있습니다.");
        }

        // 서비스 호출
        isvc.registerAnswer(id, answerContent);

        // 등록 후 다시 상세 페이지로 돌아가서 확인
        return "redirect:/inquiry/detail/" + id;
    }
}
