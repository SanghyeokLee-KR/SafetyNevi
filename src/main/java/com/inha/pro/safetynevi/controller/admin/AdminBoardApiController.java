package com.inha.pro.safetynevi.controller.admin;

import com.inha.pro.safetynevi.dto.map.BoardDto;
import com.inha.pro.safetynevi.service.map.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 게시글 관리 API
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBoardApiController {

    private final BoardService boardService;

    // 게시글 상세 정보 조회 (신고 내역 확인용)
    @GetMapping("/board/{id}")
    public ResponseEntity<?> getBoardById(@PathVariable Long id) {
        // 관리자는 본인 확인 없이 조회 가능하므로 userCheck=null 처리
        BoardDto dto = boardService.getBoardDetail(id, null);
        return ResponseEntity.ok(dto);
    }
}