package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 전체 글 + 작성자/댓글 fetch join. likes는 카테시안 곱 방지 위해 BatchSize 지연로딩
    @Query("SELECT DISTINCT b FROM Board b " +
            "LEFT JOIN FETCH b.writer " +
            "LEFT JOIN FETCH b.comments " +
            "ORDER BY b.createdAt DESC")
    List<Board> findAllWithAllAssociations();

    // 특정 사용자가 작성한 게시글 조회 (likes는 지연로딩)
    @Query("SELECT DISTINCT b FROM Board b " +
            "LEFT JOIN FETCH b.writer " +
            "LEFT JOIN FETCH b.comments " +
            "WHERE b.writer.userId = :userId " +
            "ORDER BY b.createdAt DESC")
    List<Board> findAllByWriterWithAssociations(@Param("userId") String userId);
}