package com.inha.pro.safetynevi.infra.lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SchedulerLockRepository extends JpaRepository<SchedulerLock, String> {

    /** 만료(=비점유)된 락만 원자적으로 점유. 갱신된 행 수가 1이면 획득 성공. */
    @Modifying
    @Query("update SchedulerLock s set s.lockedUntil = :until, s.lockedBy = :by " +
           "where s.name = :name and s.lockedUntil < :now")
    int acquire(@Param("name") String name, @Param("now") Instant now,
                @Param("until") Instant until, @Param("by") String by);

    /** 내가 점유한 락만 즉시 만료 처리(해제). */
    @Modifying
    @Query("update SchedulerLock s set s.lockedUntil = :now " +
           "where s.name = :name and s.lockedBy = :by")
    int release(@Param("name") String name, @Param("now") Instant now, @Param("by") String by);
}
