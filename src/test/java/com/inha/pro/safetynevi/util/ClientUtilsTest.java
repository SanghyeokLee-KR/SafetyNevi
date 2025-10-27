package com.inha.pro.safetynevi.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientUtilsTest {

    @Test
    void trustProxy_꺼져있으면_XFF는_무시하고_직접연결IP를_쓴다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "1.2.3.4"); // 위조 시도

        assertThat(ClientUtils.getRemoteIP(req, false)).isEqualTo("10.0.0.5");
    }

    @Test
    void 단일인자_getRemoteIP는_기본적으로_XFF를_신뢰하지_않는다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(ClientUtils.getRemoteIP(req)).isEqualTo("10.0.0.5");
    }

    @Test
    void trustProxy_켜져있으면_XFF_맨앞_IP를_쓴다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2"); // client, proxy 체인

        assertThat(ClientUtils.getRemoteIP(req, true)).isEqualTo("1.1.1.1");
    }

    @Test
    void trustProxy_켜져도_XFF가_없으면_직접연결IP를_쓴다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");

        assertThat(ClientUtils.getRemoteIP(req, true)).isEqualTo("10.0.0.5");
    }

    @Test
    void IPv6_로컬호스트는_127001로_정규화한다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("0:0:0:0:0:0:0:1");

        assertThat(ClientUtils.getRemoteIP(req, false)).isEqualTo("127.0.0.1");
    }
}
