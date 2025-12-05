package com.inha.pro.safetynevi.dao.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisasterMessageRepository extends JpaRepository<DisasterMessage, Long>, JpaSpecificationExecutor<DisasterMessage> {

    // 공식 API 일련번호(SN) 기준 중복 검사
    boolean existsBySn(Long sn);

    DisasterMessage findTopByOrderByDmidDesc();

    // '발송지역' 드롭다운 메뉴를 채우기 위한 메소드 (이것을 추가)
    @Query("SELECT DISTINCT " +
            "CASE " +
            "  WHEN d.area LIKE '경상남도%' OR d.area LIKE '경상북도%' OR d.area LIKE '충청남도%' OR d.area LIKE '충청북도%' OR d.area LIKE '전라남도%' OR d.area LIKE '전라북도%' THEN SUBSTRING(d.area, 1, 4) " +
            "  WHEN d.area LIKE '경기도%' THEN SUBSTRING(d.area, 1, 3) " +
            "  ELSE SUBSTRING(d.area, 1, 2) " +
            "END " +
            "FROM DisasterMessage d ORDER BY 1")
    List<String> findDistinctAreaPrefixes();

    // '재난종류' 드롭다운 메뉴를 채우기 위한 메소드 (이 부분을 추가)
    @Query("SELECT DISTINCT d.disasterType FROM DisasterMessage d ORDER BY d.disasterType")
    List<String> findDistinctDisasterTypes();

    // 사이드바 피드 무한 스크롤: 커서(발령시각 sentDate, dmid)보다 과거 문자만 최신순으로.
    // sentDate 포맷이 'yyyy/MM/dd HH:mm:ss'라 문자열 비교가 시간순과 일치한다. dmid로 동시각 타이브레이크.
    @Query("SELECT m FROM DisasterMessage m " +
            "WHERE m.sentDate < :date OR (m.sentDate = :date AND m.dmid < :id) " +
            "ORDER BY m.sentDate DESC, m.dmid DESC")
    List<DisasterMessage> findOlderThan(@Param("date") String date, @Param("id") Long id, Pageable pageable);

    // (findAllByOrderByDmidDesc 와 findAll(Pageable) 은 삭제)
}
