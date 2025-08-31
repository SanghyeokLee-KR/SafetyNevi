package com.inha.pro.safetynevi.dao.member;

import com.inha.pro.safetynevi.entity.member.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    // 최근 접속 이력 20건 조회 (마이페이지 노출용)
    List<AccessLog> findTop20ByUserIdOrderByLogDateDesc(String userId);
}