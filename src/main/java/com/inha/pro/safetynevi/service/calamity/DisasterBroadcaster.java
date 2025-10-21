package com.inha.pro.safetynevi.service.calamity;

import com.inha.pro.safetynevi.dto.calamity.DisasterZoneResponse;

public interface DisasterBroadcaster {
    void broadcastNew(DisasterZoneResponse zone);
    void broadcastDelete(Long id);
}
