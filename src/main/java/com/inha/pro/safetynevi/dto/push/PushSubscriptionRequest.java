package com.inha.pro.safetynevi.dto.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 브라우저 PushSubscription.toJSON() 형태 그대로 받는다: { endpoint, expirationTime, keys: { p256dh, auth } }.
// expirationTime 등 안 쓰는 필드는 무시.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushSubscriptionRequest(String endpoint, Keys keys) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Keys(String p256dh, String auth) {
    }
}
