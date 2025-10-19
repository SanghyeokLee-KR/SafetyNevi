package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;

/**
 * 재난 발생/삭제를 클라이언트로 전파하는 전략.
 * - 비운영(단일 인스턴스): WebSocket으로 바로 전파 (DirectDisasterBroadcaster)
 * - 운영(HA): Kafka 토픽으로 발행 → 모든 인스턴스가 소비해 각자 전파 (KafkaDisasterBroadcaster)
 */
public interface DisasterBroadcaster {
    void broadcastNew(DisasterZoneResponse zone);
    void broadcastDelete(Long id);
}
