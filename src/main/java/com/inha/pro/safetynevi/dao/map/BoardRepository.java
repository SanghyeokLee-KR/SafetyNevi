package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 전체 게시글 조회 (작성자, 댓글, 좋아요 Fetch Join)
    @Query("SELECT DISTINCT b FROM Board b " +
            "LEFT JOIN FETCH b.writer " +
            "LEFT JOIN FETCH b.comments " +
            "LEFT JOIN FETCH b.likes " +
            "ORDER BY b.createdAt DESC")
    List<Board> findAllWithAllAssociations();

    // 특정 사용자가 작성한 게시글 조회
    @Query("SELECT DISTINCT b FROM Board b " +
            "LEFT JOIN FETCH b.writer " +
            "LEFT JOIN FETCH b.comments " +
            "LEFT JOIN FETCH b.likes " +
            "WHERE b.writer.userId = :userId " +
            "ORDER BY b.createdAt DESC")
    List<Board> findAllByWriterWithAssociations(@Param("userId") String userId);
}