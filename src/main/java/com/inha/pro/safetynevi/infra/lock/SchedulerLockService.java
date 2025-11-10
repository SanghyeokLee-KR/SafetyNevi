package com.inha.pro.safetynevi.infra.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

/**
 * DB 기반 분산 락. 여러 인스턴스가 떠도 같은 스케줄 작업이 한 곳에서만 실행되게 한다.
 * 락 행은 시작 시 {@link SchedulerLockInitializer} 가 미리 만들어 두므로
 * 여기서는 순수 UPDATE(획득/해제)만 수행한다 → 충돌로 인한 트랜잭션 오염이 없다.
 */
@Service
@RequiredArgsConstructor
public class SchedulerLockService {

    private final SchedulerLockRepository repository;
    private final String instanceId = ManagementFactory.getRuntimeMXBean().getName(); // pid@host

    /** ttl 동안 락 점유 시도. 성공하면 true(이 인스턴스가 작업 수행). */
    @Transactional
    public boolean tryAcquire(String name, Duration ttl) {
        Instant now = Instant.now();
        return repository.acquire(name, now, now.plus(ttl), instanceId) == 1;
    }

    /** 작업 완료 후 해제(다음 주기에 어느 인스턴스든 다시 획득 가능). */
    @Transactional
    public void release(String name) {
        repository.release(name, Instant.now(), instanceId);
    }
}
