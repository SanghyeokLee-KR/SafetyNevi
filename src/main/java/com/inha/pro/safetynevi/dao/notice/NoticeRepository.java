package com.inha.pro.safetynevi.dao.notice;

import com.inha.pro.safetynevi.entity.notice.NoticeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {
    // 제목 키워드 검색
    Page<NoticeEntity> findByTitleContaining(String keyword, Pageable pageable);

    // 중요 > 긴급 > 일반 순, 같은 등급은 최신(id DESC)순
    @Query("SELECT n FROM NoticeEntity n " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR n.title LIKE %:keyword%) " +
            "ORDER BY " +
            "CASE WHEN n.type = 'IMPORTANT' THEN 1 " +
            "     WHEN n.type = 'EMERGENCY' THEN 2 " +
            "     ELSE 3 END ASC, " +
            "n.id DESC")
    Page<NoticeEntity> findNoticeListWithCustomSort(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("update NoticeEntity n set n.viewCount = n.viewCount + 1 where n.id = :id")
    int updateViewCount(@Param("id") Long id);
}
