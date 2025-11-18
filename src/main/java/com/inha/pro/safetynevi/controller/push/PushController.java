package com.inha.pro.safetynevi.controller.push;

import com.inha.pro.safetynevi.dto.push.PushConfigResponse;
import com.inha.pro.safetynevi.dto.push.PushSubscriptionRequest;
import com.inha.pro.safetynevi.service.push.WebPushService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

// 재난 웹푸시 구독 API. 프론트(push.ts)가 PushManager 로 구독을 만들어 등록/해지하고,
// 구독에 필요한 VAPID 공개키를 내려받는다.
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final WebPushService webPushService;

    @Value("${webpush.vapid.public-key:}")
    private String publicKey;

    // 프론트 PushManager.subscribe 용 VAPID 공개키. 발송기와 키가 갖춰져야 enabled=true.
    @GetMapping("/config")
    public PushConfigResponse config() {
        boolean enabled = webPushService.isEnabled() && publicKey != null && !publicKey.isBlank();
        return new PushConfigResponse(enabled, publicKey);
    }

    // 구독 등록. 로그인 상태면 회원과 연결, 아니면 익명으로 저장.
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest request,
                                          Principal principal,
                                          HttpServletRequest http) {
        if (request == null || request.endpoint() == null || request.keys() == null) {
            return ResponseEntity.badRequest().build();
        }
        String userId = (principal != null) ? principal.getName() : null;
        webPushService.subscribe(request.endpoint(), request.keys().p256dh(), request.keys().auth(),
                request.region(), userId, http.getHeader("User-Agent"));
        return ResponseEntity.ok().build();
    }

    // 구독 해지
    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody PushSubscriptionRequest request) {
        if (request != null && request.endpoint() != null) {
            webPushService.unsubscribe(request.endpoint());
        }
        return ResponseEntity.ok().build();
    }
}
