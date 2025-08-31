package com.inha.pro.safetynevi.service.report;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.dao.report.ReportRepository;
import com.inha.pro.safetynevi.dto.report.ReportRequestDto;
import com.inha.pro.safetynevi.entity.member.Member;
import com.inha.pro.safetynevi.entity.report.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고(Report) 관리 서비스
 * - 신고 접수, 목록 조회(페이징), 상태 변경
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    // 신고 접수
    public void createReport(String reporterId, ReportRequestDto dto) {
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .targetUser(dto.getTargetUser())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status("RECEIVED")
                .build();

        reportRepository.save(report);
    }

    // 전체 신고 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<Report> getAllReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 신고 처리 상태 변경
    public void updateReportStatus(Long id, String status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        report.updateStatus(status);
    }
}