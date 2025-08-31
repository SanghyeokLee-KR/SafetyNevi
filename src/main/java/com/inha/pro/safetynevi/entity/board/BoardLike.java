package com.inha.pro.safetynevi.entity.board;

import com.inha.pro.safetynevi.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 좋아요 매핑 엔티티
 */
@Entity
@Table(name = "SAFETY_BOARD_LIKE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BoardLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LIKE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOARD_ID")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private Member user;
}