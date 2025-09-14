package com.inha.pro.safetynevi.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 시도 횟수 추적 (brute-force 방어)
 * - 같은 아이디로 5회 실패하면 10분간 잠근다. (인메모리, 단일 인스턴스 기준)
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L; // 10분

    private static class Attempt {
        int count;
        long lockedUntil;
    }

    private final Map<String, Attempt> cache = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        if (username == null || username.isBlank()) return;
        Attempt a = cache.computeIfAbsent(key(username), k -> new Attempt());
        synchronized (a) {
            if (a.lockedUntil > System.currentTimeMillis()) return; // 이미 잠겨 있으면 카운트 유지
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            }
        }
    }

    public void loginSucceeded(String username) {
        if (username != null) cache.remove(key(username));
    }

    public boolean isBlocked(String username) {
        if (username == null || username.isBlank()) return false;
        Attempt a = cache.get(key(username));
        if (a == null) return false;
        if (a.lockedUntil > System.currentTimeMillis()) return true;
        if (a.lockedUntil > 0) cache.remove(key(username)); // 잠금 만료 → 초기화
        return false;
    }

    private String key(String username) {
        return username.toLowerCase();
    }
}
