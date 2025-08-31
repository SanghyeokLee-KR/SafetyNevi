package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.BoardDto;
import com.inha.pro.safetynevi.dto.map.BoardRequestDto;
import com.inha.pro.safetynevi.service.map.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 게시글 API 및 웹소켓 메시지 전송 컨트롤러
 */
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final SimpMessagingTemplate messagingTemplate;

    // 전체 게시글 조회
    @GetMapping
    public ResponseEntity<List<BoardDto>> getBoards(@AuthenticationPrincipal User user) {
        String userId = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(boardService.getAllBoards(userId));
    }

    // 내 작성 글 조회
    @GetMapping("/my")
    public ResponseEntity<List<BoardDto>> getMyBoards(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(boardService.getMyBoards(user.getUsername()));
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardDetail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        String userId = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(boardService.getBoardDetail(id, userId));
    }

    // 게시글 작성 (웹소켓 알림 전송)
    @PostMapping
    public ResponseEntity<String> createBoard(
            @Valid @ModelAttribute BoardRequestDto dto,
            @AuthenticationPrincipal User user) {

        BoardDto newBoard = boardService.createBoardReturnDto(
                user.getUsername(),
                dto.getTitle(),
                dto.getContent(),
                dto.getCategory(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getLocationType(),
                dto.getImageFile()
        );

        messagingTemplate.convertAndSend("/topic/board/new", newBoard);
        return ResponseEntity.ok("created");
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBoard(@PathVariable Long id, @AuthenticationPrincipal User user) {
        boardService.deleteBoard(id, user.getUsername());
        messagingTemplate.convertAndSend("/topic/board/delete", id);
        return ResponseEntity.ok("deleted");
    }

    // 좋아요 토글
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable Long id, @AuthenticationPrincipal User user) {
        boolean liked = boardService.toggleLike(id, user.getUsername());
        int total = boardService.getLikeCount(id);

        messagingTemplate.convertAndSend("/topic/board/like", Map.of("boardId", id, "totalLikes", total));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // 댓글 작성
    @PostMapping("/{id}/comment")
    public ResponseEntity<BoardDto.CommentDto> addComment(@PathVariable Long id, @RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {
        Long parentId = payload.containsKey("parentId") ? Long.valueOf(payload.get("parentId").toString()) : null;

        BoardDto.CommentDto comment = boardService.addComment(id, user.getUsername(), (String)payload.get("content"), parentId);

        messagingTemplate.convertAndSend("/topic/board/comment",
                Map.of("boardId", id, "comment", comment, "parentId", parentId != null ? parentId : -1));
        return ResponseEntity.ok(comment);
    }
}