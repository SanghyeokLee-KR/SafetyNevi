package com.inha.pro.safetynevi.service.push;

import com.inha.pro.safetynevi.entity.push.PushSubscription;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 지역 구독 매칭(provinceOf·filterByRegion·normalizeRegion)과 구독 endpoint 보안 단위 테스트. 순수 로직이라 DB·목 불필요.
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

    @Test
    void isSafePushEndpoint_공인_https만_허용하고_내부주소는_막는다() {
        // 정상 푸시 서비스 (브라우저 PushManager 가 발급하는 형태)
        assertTrue(WebPushService.isSafePushEndpoint("https://fcm.googleapis.com/fcm/send/abc123"));
        assertTrue(WebPushService.isSafePushEndpoint("https://updates.push.services.mozilla.com/wpush/v2/xyz"));

        // SSRF 우려, http·로컬·내부망·메타데이터
        assertFalse(WebPushService.isSafePushEndpoint("http://fcm.googleapis.com/x"), "http 거부");
        assertFalse(WebPushService.isSafePushEndpoint("https://localhost/x"), "localhost 거부");
        assertFalse(WebPushService.isSafePushEndpoint("https://127.0.0.1/x"), "루프백 IP 거부");
        assertFalse(WebPushService.isSafePushEndpoint("https://10.0.0.5/x"), "사설 IP 거부");
        assertFalse(WebPushService.isSafePushEndpoint("https://169.254.169.254/latest/meta-data/"), "메타데이터 IP 거부");
        assertFalse(WebPushService.isSafePushEndpoint("https://[::1]/x"), "IPv6 루프백 거부");
        assertFalse(WebPushService.isSafePushEndpoint(""), "빈 값 거부");
        assertFalse(WebPushService.isSafePushEndpoint(null), "null 거부");
    }

    @Test
    void normalizeRegion_알려진_시도와_전국만_남긴다() {
        assertEquals("전국", WebPushService.normalizeRegion("전국"));
        assertEquals("인천", WebPushService.normalizeRegion("인천광역시 미추홀구"));
        assertEquals("경북", WebPushService.normalizeRegion("경상북도 고령군"));
        assertNull(WebPushService.normalizeRegion(""), "빈 값은 전국(null)");
        assertNull(WebPushService.normalizeRegion(null));
        assertNull(WebPushService.normalizeRegion("<script>alert(1)</script>"), "모르는 값은 전국(null)으로");
    }

    private PushSubscription sub(String region) {
        PushSubscription s = new PushSubscription();
        s.setAreaName(region);
        return s;
    }
}
