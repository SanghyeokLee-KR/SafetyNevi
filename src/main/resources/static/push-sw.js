// 안전네비 서비스워커 — 웹푸시 수신 + PWA 오프라인 캐싱(앱 셸·재난 대피요령).

const CACHE = 'safetynevi-v3';
// 통신이 끊겨도 열려야 하는 것들(대피요령 + 셸). 일부가 없어도 설치가 깨지지 않게 개별 캐시.
const PRECACHE = [
    '/disasterGuide',
    '/css/disaster/disasterGuide.css',
    '/css/base/tokens.css',
    '/css/base/components.css',
    '/img/logo/app-icon.png',
];

self.addEventListener('install', function (event) {
    event.waitUntil(
        caches.open(CACHE).then(function (cache) {
            return Promise.all(PRECACHE.map(function (url) {
                return cache.add(url).catch(function () { /* 일부 실패 무시 */ });
            }));
        }).then(function () { return self.skipWaiting(); })
    );
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (keys) {
            return Promise.all(keys.filter(function (k) { return k !== CACHE; })
                .map(function (k) { return caches.delete(k); }));
        }).then(function () { return self.clients.claim(); })
    );
});

// 같은 출처 GET 만 캐시. API(/api)는 항상 네트워크.
// 페이지 이동(navigate, HTML)은 network-first — 로그인 상태·관심지역 등 매 요청 서버 렌더가 항상 최신이게(오프라인이면 캐시 폴백).
// JS/CSS는 network-first(+캐시 폴백) — 코드 바꾸면 즉시 최신(옛 화면 stale 방지). 이미지는 cache-first(+백그라운드 갱신) — 잘 안 바뀌고 수가 많아 속도 우선.
self.addEventListener('fetch', function (event) {
    const req = event.request;
    if (req.method !== 'GET') return;
    const url = new URL(req.url);
    if (url.origin !== self.location.origin) return;
    if (url.pathname.startsWith('/api/')) return;

    // HTML 페이지 이동: 네트워크 우선. 끊겼을 때만 캐시(없으면 대피요령 셸)로 폴백.
    if (req.mode === 'navigate') {
        event.respondWith(
            fetch(req).then(function (res) {
                if (res && res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE).then(function (c) { c.put(req, copy); });
                }
                return res;
            }).catch(function () {
                return caches.match(req).then(function (c) { return c || caches.match('/disasterGuide'); });
            })
        );
        return;
    }

    // JS/CSS: 네트워크 우선 + 캐시 폴백. 온라인이면 항상 최신, 오프라인이면 캐시.
    if (url.pathname.endsWith('.js') || url.pathname.endsWith('.css')) {
        event.respondWith(
            fetch(req).then(function (res) {
                if (res && res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE).then(function (c) { c.put(req, copy); });
                }
                return res;
            }).catch(function () { return caches.match(req); })
        );
        return;
    }

    // 그 외(이미지 등): 캐시 우선 + 백그라운드 갱신
    event.respondWith(
        caches.match(req).then(function (cached) {
            const network = fetch(req).then(function (res) {
                if (res && res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE).then(function (c) { c.put(req, copy); });
                }
                return res;
            }).catch(function () { return cached; });
            return cached || network;
        })
    );
});

// === 웹푸시 수신 (페이지가 닫혀 있어도 동작) ===
self.addEventListener('push', function (event) {
    let data = {};
    try {
        data = event.data ? event.data.json() : {};
    } catch (e) {
        data = { body: event.data ? event.data.text() : '' };
    }
    const title = data.title || '안전네비 재난 알림';
    event.waitUntil(
        self.registration.showNotification(title, {
            body: data.body || '',
            icon: '/img/logo/favicon.png',
            data: { url: data.url || '/disasterMessage' },
        })
    );
});

// 알림을 누르면 열린 탭이 있으면 그 탭으로, 없으면 새 탭으로 재난문자 페이지를 연다.
self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    const target = (event.notification.data && event.notification.data.url) || '/disasterMessage';
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windowClients) {
            for (const client of windowClients) {
                if (client.url.indexOf(target) !== -1 && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(target);
            }
        })
    );
});
