package com.inha.pro.safetynevi.dao.map;

import com.inha.pro.safetynevi.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    // 운영 중인 병원만 조회 (지도 '병원' 필터용)
    @Query("SELECT h FROM Hospital h WHERE " +
            "h.operatingStatus = '영업/정상' AND " +
            "h.latitude BETWEEN :swLat AND :neLat AND " +
            "h.longitude BETWEEN :swLng AND :neLng")
    List<Hospital> findOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng
    );

    // 폐업/휴업 등 비정상 운영 병원 조회 (지도 '기타' 필터용)
    @Query("SELECT h FROM Hospital h WHERE " +
            "h.operatingStatus IN ('폐업', '휴업', '취소/말소/만료/정지/중지') AND " +
            "h.latitude BETWEEN :swLat AND :neLat AND " +
            "h.longitude BETWEEN :swLng AND :neLng")
    List<Hospital> findNonOperationalInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng
    );
}