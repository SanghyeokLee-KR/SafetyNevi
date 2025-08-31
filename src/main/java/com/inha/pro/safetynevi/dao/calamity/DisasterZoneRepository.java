package com.inha.pro.safetynevi.dao.calamity;

import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DisasterZoneRepository extends JpaRepository<DisasterZone, Long> {

    /**
     * 만료 시간(expiryTime)이 현재 시간보다 이후인(아직 유효한) 재난 구역 조회
     */
    List<DisasterZone> findByExpiryTimeAfter(Instant currentTime);
}