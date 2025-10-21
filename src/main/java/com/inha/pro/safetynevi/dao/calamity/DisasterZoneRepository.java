package com.inha.pro.safetynevi.dao.calamity;

import com.inha.pro.safetynevi.entity.calamity.DisasterZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DisasterZoneRepository extends JpaRepository<DisasterZone, Long> {

    // 아직 안 만료된(유효한) 구역만
    List<DisasterZone> findByExpiryTimeAfter(Instant currentTime);
}