package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.map.BoardDto;
import com.inha.pro.safetynevi.service.map.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBoardApiController {

    private final BoardService boardService;

    // 신고 들어온 글 확인용
    @GetMapping("/board/{id}")
    public ResponseEntity<?> getBoardById(@PathVariable Long id) {
        // 관리자는 본인 글 아니어도 보니까 userId는 null로
        BoardDto dto = boardService.getBoardDetail(id, null);
        return ResponseEntity.ok(dto);
    }

    // 관리자 게시물 삭제 — principal(관리자)로 권한 통과
    @DeleteMapping("/board/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id, java.security.Principal principal) {
        boardService.deleteBoard(id, principal != null ? principal.getName() : null);
        return ResponseEntity.ok().build();
    }
}