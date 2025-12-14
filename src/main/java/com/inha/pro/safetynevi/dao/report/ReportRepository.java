package com.inha.pro.safetynevi.dao.report;

import com.inha.pro.safetynevi.entity.report.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 특정 유저의 신고 내역
    List<Report> findAllByReporter_UserId(String userId);

    // 특정 유형(게시글/시설 등)의 신고 내역
    List<Report> findAllByTargetType(String targetType);

    // 페이징 전체 조회 (최신순), 신고자를 fetch join으로 함께 로드해 N+1 방지
    @Query(value = "SELECT r FROM Report r JOIN FETCH r.reporter ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Report r")
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
}