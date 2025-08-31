package com.inha.pro.safetynevi.dao.crawling;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisasterMessageRepository extends JpaRepository<DisasterMessage, Long>, JpaSpecificationExecutor<DisasterMessage> {

    // 크롤링 서비스에서 중복 검사를 위해 사용하는 메소드 (이건 유지)
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

    // (findAllByOrderByDmidDesc 와 findAll(Pageable) 은 삭제)
}
