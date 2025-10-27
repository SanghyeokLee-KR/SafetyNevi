package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.BoardLikeRepository;
import com.inha.pro.safetynevi.dao.map.BoardRepository;
import com.inha.pro.safetynevi.dao.map.CommentRepository;
import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.entity.board.Board;
import com.inha.pro.safetynevi.entity.board.BoardLike;
import com.inha.pro.safetynevi.entity.member.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock BoardRepository boardRepository;
    @Mock CommentRepository commentRepository;
    @Mock BoardLikeRepository boardLikeRepository;
    @Mock MemberRepository memberRepository;
    @InjectMocks BoardService boardService;

    // 동시에 좋아요가 두 번 들어와 유니크 제약을 위반해도, 예외를 삼키고 '좋아요됨'으로 처리한다.
    @Test
    void 동시좋아요_유니크위반은_삼키고_true를_반환한다() {
        Board board = mock(Board.class);
        Member member = mock(Member.class);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findById("u")).thenReturn(Optional.of(member));
        when(boardLikeRepository.findByBoardAndUser(board, member)).thenReturn(Optional.empty());
        when(boardLikeRepository.save(any(BoardLike.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        boolean liked = boardService.toggleLike(1L, "u");

        assertThat(liked).isTrue(); // 예외 전파 없이 좋아요 처리
    }

    @Test
    void 이미_좋아요한_상태면_취소하고_false를_반환한다() {
        Board board = mock(Board.class);
        Member member = mock(Member.class);
        BoardLike existing = mock(BoardLike.class);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findById("u")).thenReturn(Optional.of(member));
        when(boardLikeRepository.findByBoardAndUser(board, member)).thenReturn(Optional.of(existing));

        boolean liked = boardService.toggleLike(1L, "u");

        assertThat(liked).isFalse();
        verify(boardLikeRepository).delete(existing);
    }
}
