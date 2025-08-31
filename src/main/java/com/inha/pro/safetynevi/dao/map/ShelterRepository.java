package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    // [신규] '사용중'인 대피소만 검색 (대피소 체크박스용)
    @Query("SELECT s FROM Shelter s WHERE " +
            "s.operatingStatus = '사용중' AND " +
            "s.latitude BETWEEN :swLat AND :neLat AND " +
            "s.longitude BETWEEN :swLng AND :neLng")
    List<Shelter> findOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng
    );

    // [신규] '사용중'이 아닌 대피소만 검색 (기타 체크박스용)
    @Query("SELECT s FROM Shelter s WHERE " +
            "s.operatingStatus IN ('사용중지', '일시중지') AND " +
            "s.latitude BETWEEN :swLat AND :neLat AND " +
            "s.longitude BETWEEN :swLng AND :neLng")
    List<Shelter> findNonOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng
    );
}