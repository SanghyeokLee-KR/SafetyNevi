package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}