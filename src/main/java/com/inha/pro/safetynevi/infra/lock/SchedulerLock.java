package com.inha.pro.safetynevi.infra.lock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 분산 환경에서 "한 인스턴스만 작업 수행"을 보장하는 DB 기반 락 행.
 * (ShedLock의 최소 구현, 운영 규모가 커지면 ShedLock/Redis 분산락으로 교체 가능)
 */
@Entity
@Table(name = "scheduler_lock")
@Getter
@Setter
@NoArgsConstructor
public class SchedulerLock {

    @Id
    @Column(length = 64)
    private String name;          // 락 이름 (예: disasterCrawl)

    private Instant lockedUntil;  // 이 시각까지 점유. 지나면 다른 인스턴스가 획득 가능

    @Column(length = 100)
    private String lockedBy;      // 현재 점유 중인 인스턴스 식별자(pid@host)

    public SchedulerLock(String name) {
        this.name = name;
        this.lockedUntil = Instant.EPOCH;   // 최초엔 비점유 상태
    }
}
