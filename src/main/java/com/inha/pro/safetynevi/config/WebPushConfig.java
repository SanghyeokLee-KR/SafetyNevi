package com.inha.pro.safetynevi.config;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

// 표준 Web Push(VAPID) 발송기. VAPID 키쌍(webpush.vapid.*)이 있을 때만 PushService 빈을 만든다.
// 키가 없으면 null 빈 → 발송만 꺼지고 앱은 정상 동작(키 미설정 환경 대비).
@Slf4j
@Configuration
public class WebPushConfig {

    @Bean
    public PushService pushService(
            @Value("${webpush.vapid.public-key:}") String publicKey,
            @Value("${webpush.vapid.private-key:}") String privateKey,
            @Value("${webpush.vapid.subject:mailto:admin@safetynevi.local}") String subject) {

        if (publicKey.isBlank() || privateKey.isBlank()) {
            log.info("VAPID 키 미설정 — 웹푸시 발송 비활성화");
            return null;
        }

        // web-push 의 페이로드 암호화(ECDH/HKDF)에 BouncyCastle 프로바이더가 필요하다.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        try {
            PushService service = new PushService(publicKey, privateKey, subject);
            log.info("웹푸시(VAPID) 초기화 완료 — 재난 푸시 발송 활성화");
            return service;
        } catch (Exception e) {
            log.error("웹푸시 초기화 실패 — 비활성화: {}", e.getMessage());
            return null;
        }
    }
}
