package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.Shelter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    // '사용중'인 대피소만 (대피소 체크박스용)
    @Query("SELECT s FROM Shelter s WHERE " +
            "s.operatingStatus = '사용중' AND " +
            "s.latitude BETWEEN :swLat AND :neLat AND " +
            "s.longitude BETWEEN :swLng AND :neLng")
    List<Shelter> findOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            Pageable pageable
    );

    // 사용중 아닌 대피소 (기타 체크박스용)
    @Query("SELECT s FROM Shelter s WHERE " +
            "s.operatingStatus IN ('사용중지', '일시중지') AND " +
            "s.latitude BETWEEN :swLat AND :neLat AND " +
            "s.longitude BETWEEN :swLng AND :neLng")
    List<Shelter> findNonOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            Pageable pageable
    );

    // 현재 위치 주변(위경도 박스) 대피소 전체 조회 — 경로 추천 후보용
    @Query("SELECT s FROM Shelter s WHERE " +
            "s.latitude BETWEEN :minLat AND :maxLat AND " +
            "s.longitude BETWEEN :minLng AND :maxLng")
    List<Shelter> findAllInBounds(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );
}