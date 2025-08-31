package com.inha.pro.safetynevi.dto.map;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 게시글 상세 정보 응답 DTO
 * - 게시글 정보, 작성자, 좋아요 상태, 댓글 목록(계층 구조) 포함
 */
@Data @Builder
public class BoardDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String writer;
    private Double latitude;
    private Double longitude;
    private String date;
    private String imageUrl;
    private int likeCount;
    private boolean liked;       // 현재 사용자의 좋아요 여부
    private String locationType; // GPS 인증 여부
    private boolean canDelete;   // 삭제 권한 보유 여부

    private List<CommentDto> comments;

    // 댓글 DTO (Inner Class)
    @Data @Builder
    public static class CommentDto {
        private Long id;
        private String writer;
        private String content;
        private String timeAgo;
        private List<CommentDto> replies; // 대댓글 리스트
    }
}