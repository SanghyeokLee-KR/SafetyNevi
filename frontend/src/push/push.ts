// 재난 웹푸시 구독/해지. 버튼을 누르면 (미구독)→권한 받고 구독 등록 / (구독중)→해지.
// VAPID 공개키는 /api/push/config 에서 받아온다. 외부 SDK 없이 브라우저 내장 API만 쓴다.
(function () {
    const btn = document.getElementById('btn-push-subscribe') as HTMLButtonElement | null;
    if (!btn) return;

    const defaultLabel = btn.textContent;
    let subscribed = false;
    // 온보딩(QR) 모드: 이미 구독 중이어도 '이 지역으로' 다시 눌러 지역을 갱신할 수 있게 한다.
    const onboardMode = btn.dataset.mode === 'onboard';

    const supported = ('serviceWorker' in navigator) && ('PushManager' in window) && ('Notification' in window);
    if (!supported) {
        btn.disabled = true;
        btn.textContent = '이 브라우저는 푸시 알림을 지원하지 않아요';
        return;
    }

    function reset(message?: string) {
        btn.disabled = false;
        btn.textContent = message || defaultLabel;
        btn.title = '';
        subscribed = false;
    }

    function markSubscribed() {
        subscribed = true;
        btn.disabled = false;
        btn.textContent = '✓ 알림 구독중';
        btn.title = '클릭하면 알림 해지';
    }

    // VAPID 공개키(base64url) → PushManager 가 요구하는 Uint8Array (ArrayBuffer 백킹).
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

            let subscription = await registration.pushManager.getSubscription();
            if (!subscription) {
                subscription = await registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(cfg.publicKey),
                });
            }

            // PushSubscription.toJSON() = { endpoint, expirationTime, keys: { p256dh, auth } } + 선택 지역
            const subJson = subscription.toJSON();
            const regionSel = document.getElementById('push-region') as HTMLSelectElement | null;
            const res = await fetch('/api/push/subscribe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ endpoint: subJson.endpoint, keys: subJson.keys, region: regionSel ? regionSel.value : '전국' }),
            });
            if (!res.ok) throw new Error('서버 등록 실패: ' + res.status);

            markSubscribed();
        } catch (e) {
            console.error('[push] 구독 실패', e);
            alert('알림 구독 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.');
            reset();
        }
    }

    async function unsubscribe() {
        btn.disabled = true;
        btn.textContent = '해지 중...';
        try {
            const registration = await navigator.serviceWorker.getRegistration();
            const subscription = registration ? await registration.pushManager.getSubscription() : null;
            if (subscription) {
                await fetch('/api/push/unsubscribe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ endpoint: subscription.endpoint }),
                });
                await subscription.unsubscribe();
            }
            reset();
        } catch (e) {
            console.error('[push] 해지 실패', e);
            reset();
        }
    }

    function handleClick() {
        // 온보딩은 해지 토글 대신 항상 (재)구독, 클릭 시 현재 #push-region 지역으로 갱신된다.
        if (subscribed && !onboardMode) unsubscribe();
        else subscribe();
    }

    // 새로고침·페이지 이동을 해도 구독은 유지된다(서비스워커가 페이지와 독립적으로 받음).
    // 이미 구독돼 있으면 버튼에 표시하고, 서버가 재시작 등으로 잊었을 수 있으니 다시 등록해 둔다.
    navigator.serviceWorker.getRegistration().then(function (registration) {
        if (!registration) return;
        registration.pushManager.getSubscription().then(function (subscription) {
            if (!subscription) return;
            if (onboardMode) return;   // 온보딩은 '이 지역으로 받기'를 다시 누르게 둔다(지역 갱신 위해)
            markSubscribed();
            fetch('/api/push/subscribe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(subscription),
            });
        });
    });

    btn.addEventListener('click', handleClick);
})();
