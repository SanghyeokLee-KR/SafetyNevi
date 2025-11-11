// 표준 Web Push 수신용 서비스워커. 페이지가 닫혀 있어도 브라우저 푸시 서비스가 push 이벤트로 깨운다.
// 외부 SDK·importScripts 없음 — 브라우저 내장 API만 사용.

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

// 알림을 누르면 이미 열린 탭이 있으면 그 탭으로, 없으면 새 탭으로 재난문자 페이지를 연다.
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
