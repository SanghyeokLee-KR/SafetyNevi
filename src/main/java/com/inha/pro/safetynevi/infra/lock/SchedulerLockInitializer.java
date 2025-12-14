package com.inha.pro.safetynevi.infra.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시작 시 락 행을 한 번 보장 생성한다. (획득/해제 경로에서 INSERT를 하지 않게 해
 * 트랜잭션 롤백-only 오염을 피한다.) 여러 인스턴스가 동시에 떠도 PK 충돌은 무시.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchedulerLockInitializer implements CommandLineRunner {

    public static final String DISASTER_CRAWL = "disasterCrawl";

    private final SchedulerLockRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        ensure(DISASTER_CRAWL);
    }

    private void ensure(String name) {
        if (repository.existsById(name)) return;     // 이미 있으면 그대로 둠(점유 상태 보존)
        try {
            repository.save(new SchedulerLock(name));
            log.info("[lock] 분산 락 행 생성: {}", name);
        } catch (DataIntegrityViolationException e) {
            // 다른 인스턴스가 먼저 만든 경우, 정상
        }
    }
}
