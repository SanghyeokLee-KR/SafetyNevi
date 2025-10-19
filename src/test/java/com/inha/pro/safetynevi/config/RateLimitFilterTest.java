package com.inha.pro.safetynevi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    // 인메모리 레이트리미터(비운영 경로)에 한도를 주입해 필터를 만든다
    private RateLimitFilter filterWithLimit(int max) {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxRequests", max);
        return new RateLimitFilter(limiter);
    }

    @Test
    void returns429WhenOverLimit() throws Exception {
        RateLimitFilter filter = filterWithLimit(2);

        assertEquals(200, callApi(filter, "1.2.3.4").getStatus());
        assertEquals(200, callApi(filter, "1.2.3.4").getStatus());
        assertEquals(429, callApi(filter, "1.2.3.4").getStatus()); // 한도 초과
    }

    @Test
    void countsPerIpSeparately() throws Exception {
        RateLimitFilter filter = filterWithLimit(1);

        assertEquals(200, callApi(filter, "1.1.1.1").getStatus());
        assertEquals(429, callApi(filter, "1.1.1.1").getStatus()); // 같은 IP 초과
        assertEquals(200, callApi(filter, "2.2.2.2").getStatus()); // 다른 IP는 영향 없음
    }

    @Test
    void doesNotLimitNonApiPaths() throws Exception {
        RateLimitFilter filter = filterWithLimit(1);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/map");
            req.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }

    private MockHttpServletResponse callApi(RateLimitFilter filter, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/facilities");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }
}
