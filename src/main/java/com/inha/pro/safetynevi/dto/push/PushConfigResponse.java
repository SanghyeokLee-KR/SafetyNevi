package com.inha.pro.safetynevi.dto.push;

// 프론트가 PushManager.subscribe 에 쓸 VAPID 공개키. enabled=false 면 아직 키가 안 들어온 상태 → 구독 버튼 비활성.
public record PushConfigResponse(boolean enabled, String publicKey) {
}
