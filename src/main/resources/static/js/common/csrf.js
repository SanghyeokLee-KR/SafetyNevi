// 페이지의 <meta name="_csrf"> 토큰을 읽어, 같은 출처로 보내는 모든 변경 요청
// (POST/PUT/DELETE/PATCH)에 CSRF 헤더를 자동으로 붙인다.
// fetch와 XMLHttpRequest(jQuery·axios 포함)를 모두 감싸므로 각 호출부를 고칠 필요가 없다.
(function () {
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) return;

    var token = tokenMeta.getAttribute('content');
    var header = headerMeta.getAttribute('content');
    if (!token || !header) return;

    var SAFE = /^(GET|HEAD|OPTIONS|TRACE)$/i;

    // 외부 API로는 토큰을 보내지 않도록 같은 출처만 처리
    function sameOrigin(url) {
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (e) {
            return true; // 상대 경로 등 파싱 불가 시 같은 출처로 간주
        }
    }

    // 1) fetch 래핑
    if (window.fetch) {
        var origFetch = window.fetch;
        window.fetch = function (input, init) {
            init = init || {};
            var url = (typeof input === 'string') ? input : (input && input.url);
            var method = init.method || (input && input.method) || 'GET';
            if (!SAFE.test(method) && sameOrigin(url)) {
                var headers = new Headers(init.headers || (input && input.headers) || {});
                if (!headers.has(header)) headers.set(header, token);
                init.headers = headers;
            }
            return origFetch.call(this, input, init);
        };
    }

    // 2) XMLHttpRequest 래핑 (jQuery, axios, 순수 XHR 모두 커버)
    if (window.XMLHttpRequest) {
        var origOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function (method, url) {
            this.__csrfNeeded = !SAFE.test(method || 'GET') && sameOrigin(url);
            return origOpen.apply(this, arguments);
        };
        var origSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function () {
            if (this.__csrfNeeded) {
                try { this.setRequestHeader(header, token); } catch (e) {}
            }
            return origSend.apply(this, arguments);
        };
    }
})();
