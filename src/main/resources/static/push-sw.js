// 안전네비 서비스워커 — 웹푸시 수신 + PWA 오프라인 캐싱(앱 셸·재난 대피요령).

const CACHE = 'safetynevi-v1';
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

// 같은 출처 GET 만 캐시. API(/api)는 항상 네트워크(최신 데이터), 그 외는 stale-while-revalidate.
// 오프라인이면 캐시로 폴백 → 한 번 본 페이지·대피요령은 통신 없이도 열린다.
self.addEventListener('fetch', function (event) {
    const req = event.request;
    if (req.method !== 'GET') return;
    const url = new URL(req.url);
    if (url.origin !== self.location.origin) return;
    if (url.pathname.startsWith('/api/')) return;

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
