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

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<BoardDto>> getBoards(@AuthenticationPrincipal User user) {
        String userId = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(boardService.getAllBoards(userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BoardDto>> getMyBoards(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(boardService.getMyBoards(user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardDetail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        String userId = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(boardService.getBoardDetail(id, userId));
    }

    // 새 글 쓰면 지도에 실시간 뜨게 웹소켓으로 알림
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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBoard(@PathVariable Long id, @AuthenticationPrincipal User user) {
        boardService.deleteBoard(id, user.getUsername());
        messagingTemplate.convertAndSend("/topic/board/delete", id);
        return ResponseEntity.ok("deleted");
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable Long id, @AuthenticationPrincipal User user) {
        boolean liked = boardService.toggleLike(id, user.getUsername());
        int total = boardService.getLikeCount(id);

        messagingTemplate.convertAndSend("/topic/board/like", Map.of("boardId", id, "totalLikes", total));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<BoardDto.CommentDto> addComment(@PathVariable Long id, @RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {

        Object pidObj = payload.get("parentId");
        Long parentId = null;

        // 프론트가 "null" 문자열로 보내거나 숫자 아닌 값을 보낼 때가 있어서 방어적으로 파싱
        if (pidObj != null) {
            String pidStr = String.valueOf(pidObj).trim();
            if (!pidStr.equalsIgnoreCase("null") && !pidStr.isEmpty()) {
                try {
                    parentId = Long.valueOf(pidStr);
                } catch (NumberFormatException e) {
                    parentId = null;
                }
            }
        }

        BoardDto.CommentDto comment = boardService.addComment(id, user.getUsername(), (String)payload.get("content"), parentId);

        // parentId 없으면 -1로 보내서 일반 댓글(대댓글 아님)임을 프론트에 표시
        messagingTemplate.convertAndSend("/topic/board/comment",
                Map.of("boardId", id, "comment", comment, "parentId", parentId != null ? parentId : -1));

        return ResponseEntity.ok(comment);
    }
}