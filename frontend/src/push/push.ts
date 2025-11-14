// 재난 웹푸시 구독 (표준 Web Push). 버튼을 누르면 알림 권한을 받고, 브라우저 PushManager 로
// 구독을 만들어 서버에 등록한다. 외부 SDK 없이 브라우저 내장 API만 쓴다.
// VAPID 공개키는 /api/push/config 에서 받아온다 — 키가 없으면 버튼을 비활성 처리한다.
(function () {
    const btn = document.getElementById('btn-push-subscribe') as HTMLButtonElement | null;
    if (!btn) return;

    const defaultLabel = btn.textContent;

    const supported = ('serviceWorker' in navigator) && ('PushManager' in window) && ('Notification' in window);
    if (!supported) {
        btn.disabled = true;
        btn.textContent = '이 브라우저는 푸시 알림을 지원하지 않아요';
        return;
    }

    function reset(message?: string) {
        btn.disabled = false;
        btn.textContent = message || defaultLabel;
    }

    // VAPID 공개키(base64url) → PushManager 가 요구하는 Uint8Array.
    // ArrayBuffer 백킹을 명시해 BufferSource 타입(applicationServerKey)을 만족시킨다.
    function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
        const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
        const raw = atob(base64);
        const output = new Uint8Array(new ArrayBuffer(raw.length));
        for (let i = 0; i < raw.length; i++) output[i] = raw.charCodeAt(i);
        return output;
    }

    async function loadConfig() {
        const res = await fetch('/api/push/config', { headers: { 'Accept': 'application/json' } });
        return res.ok ? res.json() : null;
    }

    async function subscribe() {
        btn.disabled = true;
        btn.textContent = '설정 중...';

        try {
            const cfg = await loadConfig();
            if (!cfg || !cfg.enabled || !cfg.publicKey) {
                alert('아직 푸시 서버 설정이 준비되지 않았어요. 관리자에게 문의해 주세요.');
                reset();
                return;
            }

            const permission = await Notification.requestPermission();
            if (permission !== 'granted') {
                alert('알림 권한이 거부됐어요. 브라우저 설정에서 허용해 주세요.');
                reset();
                return;
            }

            const registration = await navigator.serviceWorker.register('/push-sw.js');
            await navigator.serviceWorker.ready;

            // 이미 구독돼 있으면 그대로 재사용(중복 클릭 대비)
            let subscription = await registration.pushManager.getSubscription();
            if (!subscription) {
                subscription = await registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(cfg.publicKey),
                });
            }

            // PushSubscription.toJSON() = { endpoint, expirationTime, keys: { p256dh, auth } }
            const res = await fetch('/api/push/subscribe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(subscription),
            });
            if (!res.ok) throw new Error('서버 등록 실패: ' + res.status);

            btn.textContent = '✓ 알림 구독 완료';
        } catch (e) {
            console.error('[push] 구독 실패', e);
            alert('알림 구독 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.');
            reset();
        }
    }

    // 새로고침·페이지 이동을 해도 구독은 유지된다(서비스워커가 페이지와 독립적으로 푸시를 받음).
    // 이미 구독돼 있으면 버튼에 표시하고, 서버가 재시작 등으로 잊었을 수 있으니 다시 등록해 둔다.
    navigator.serviceWorker.getRegistration().then(function (registration) {
        if (!registration) return;
        registration.pushManager.getSubscription().then(function (subscription) {
            if (!subscription) return;
            btn.textContent = '✓ 알림 구독중';
            fetch('/api/push/subscribe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(subscription),
            });
        });
    });

    btn.addEventListener('click', subscribe);
})();
