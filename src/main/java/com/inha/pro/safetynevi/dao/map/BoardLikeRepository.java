package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.board.Board;
import com.inha.pro.safetynevi.entity.board.BoardLike;
import com.inha.pro.safetynevi.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    Optional<BoardLike> findByBoardAndUser(Board board, Member user);
}