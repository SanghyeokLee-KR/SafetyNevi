package com.inha.pro.safetynevi.service.push;

import com.inha.pro.safetynevi.entity.push.PushSubscription;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 지역 구독 매칭(provinceOf·filterByRegion) 단위 테스트. 순수 로직이라 DB·목 불필요.
class WebPushServiceTest {

    @Test
    void provinceOf_시도를_짧은형태로_추출() {
        assertEquals("대전", WebPushService.provinceOf("대전광역시"));
        assertEquals("경기", WebPushService.provinceOf("경기도 김포시"));
        assertEquals("충북", WebPushService.provinceOf("충청북도 청주시 서원구 개신동"));
        assertEquals("경남", WebPushService.provinceOf("경상남도 김해시 부곡동"));
        assertEquals("인천", WebPushService.provinceOf("인천"));
        assertNull(WebPushService.provinceOf(null));
        assertNull(WebPushService.provinceOf(""));
        assertNull(WebPushService.provinceOf("어딘가"));
    }

    @Test
    void filterByRegion_같은_시도와_전국_구독만_남긴다() {
        PushSubscription incheon = sub("인천");
        PushSubscription daejeon = sub("대전");
        PushSubscription nationwide = sub("전국");
        PushSubscription noRegion = sub(null);

        List<PushSubscription> result =
                WebPushService.filterByRegion(List.of(incheon, daejeon, nationwide, noRegion), "대전광역시");

        assertTrue(result.contains(daejeon), "같은 시/도(대전) 포함");
        assertTrue(result.contains(nationwide), "전국 구독 포함");
        assertTrue(result.contains(noRegion), "지역 미지정 구독 포함");
        assertFalse(result.contains(incheon), "다른 시/도(인천) 제외");
    }

    @Test
    void filterByRegion_시도를_못찾으면_전체에_보낸다() {
        // 원형 재난 등 지역명이 없거나 매칭 안 되면 필터하지 않고 모두에게
        PushSubscription incheon = sub("인천");
        PushSubscription daejeon = sub("대전");

        List<PushSubscription> result =
                WebPushService.filterByRegion(List.of(incheon, daejeon), "어딘가");

        assertEquals(2, result.size());
    }

    private PushSubscription sub(String region) {
        PushSubscription s = new PushSubscription();
        s.setAreaName(region);
        return s;
    }
}
