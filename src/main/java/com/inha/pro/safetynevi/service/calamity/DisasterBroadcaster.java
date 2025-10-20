package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;

// 재난 알림 전파. 로컬은 그냥 웹소켓, 운영은 카프카 거쳐서 보낸다 (구현체 둘)
public interface DisasterBroadcaster {
    void broadcastNew(DisasterZoneResponse zone);
    void broadcastDelete(Long id);
}
