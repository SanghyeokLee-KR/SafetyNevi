package com.inha.pro.safetynevi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    // 인메모리 리미터에 한도 박아서 필터 만듦 (일반 한도, 쓰기 전용 한도)
    private RateLimitFilter filterWith(int apiMax, int writeMax) {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxRequests", apiMax);
        RateLimitFilter filter = new RateLimitFilter(limiter);
        ReflectionTestUtils.setField(filter, "writePerMinute", writeMax);
        return filter;
    }

    private RateLimitFilter filterWithLimit(int max) {
        return filterWith(max, 1000); // 쓰기 한도는 넉넉히, 이 테스트들은 일반 한도만 검증
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

    @Test
    void strictLimitOnPushWriteEndpoint() throws Exception {
        RateLimitFilter filter = filterWith(100, 2); // 일반 100/분, 쓰기 2/분

        assertEquals(200, callPush(filter, "9.9.9.9").getStatus());
        assertEquals(200, callPush(filter, "9.9.9.9").getStatus());
        assertEquals(429, callPush(filter, "9.9.9.9").getStatus()); // 쓰기 전용 한도 초과
    }

    @Test
    void pushReadNotWriteLimited() throws Exception {
        RateLimitFilter filter = filterWith(100, 1); // 쓰기 한도 1이어도 GET은 영향 없음

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/push/config");
            req.setRemoteAddr("8.8.8.8");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }

    private MockHttpServletResponse callPush(RateLimitFilter filter, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/push/subscribe");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    private MockHttpServletResponse callApi(RateLimitFilter filter, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/facilities");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }
}
