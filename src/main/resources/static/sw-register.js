// PWA: 서비스워커 등록, 앱 셸·재난 대피요령 오프라인 캐싱 + 설치 가능.
// 푸시 구독과 같은 워커(/push-sw.js)를 공유한다(등록은 여러 번 불러도 안전).
if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
        navigator.serviceWorker.register('/push-sw.js').catch(function (e) {
            console.error('서비스워커 등록 실패', e);
        });
    });
}
