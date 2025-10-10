/**
 * 지도 객체 초기화 및 컨트롤러 설정
 */
export let map = null;
export let clusterer = null;

let currentCircles = [];
let watchId = null;
let isRoadviewMode = false;

// 지도 초기화
export function initMap() {
    const container = document.getElementById('map');
    const options = { center: new kakao.maps.LatLng(37.566826, 126.9786567), level: 4 };

    map = new kakao.maps.Map(container, options);
    clusterer = new kakao.maps.MarkerClusterer({ map: map, averageCenter: true, minLevel: 5, gridSize: 35 });

    const zoomControl = new kakao.maps.ZoomControl();
    map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

    setupMapControls();
    return map;
}

// 지도 컨트롤 버튼 이벤트 설정
function setupMapControls() {
    const controls = {
        traffic: document.getElementById('btn-mode-traffic'),
        terrain: document.getElementById('btn-mode-cctv'),
        skyview: document.getElementById('btn-mode-skyview'),
        dark: document.getElementById('btn-mode-dark'),
        radius: document.getElementById('btn-mode-radius'),
        track: document.getElementById('btn-mode-track'),
        sms: document.getElementById('btn-sms-report'),
        roadview: document.getElementById('btn-mode-roadview')
    };

    // 1. 교통정보 토글
    controls.traffic?.addEventListener('click', () => {
        const active = controls.traffic.classList.toggle('active');
        active ? map.addOverlayMapTypeId(kakao.maps.MapTypeId.TRAFFIC) : map.removeOverlayMapTypeId(kakao.maps.MapTypeId.TRAFFIC);
    });

    // 2. 지형도 토글
    controls.terrain?.addEventListener('click', () => {
        const active = controls.terrain.classList.toggle('active');
        active ? map.addOverlayMapTypeId(kakao.maps.MapTypeId.TERRAIN) : map.removeOverlayMapTypeId(kakao.maps.MapTypeId.TERRAIN);
    });

    // 3. 위성지도 토글
    controls.skyview?.addEventListener('click', () => {
        const active = controls.skyview.classList.toggle('active');
        map.setMapTypeId(active ? kakao.maps.MapTypeId.HYBRID : kakao.maps.MapTypeId.ROADMAP);
    });

    // 4. 로드뷰 모드
    controls.roadview?.addEventListener('click', () => {
        isRoadviewMode = !isRoadviewMode;
        if (isRoadviewMode) {
            map.addOverlayMapTypeId(kakao.maps.MapTypeId.ROADVIEW);
            controls.roadview.classList.add('active');
            map.setCursor('url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/arrow_white.png), auto');
        } else {
            map.removeOverlayMapTypeId(kakao.maps.MapTypeId.ROADVIEW);
            controls.roadview.classList.remove('active');
            map.setCursor('default');
        }
    });

    kakao.maps.event.addListener(map, 'click', function(e) {
        if (isRoadviewMode && window.openRoadview) {
            window.openRoadview(e.latLng.getLat(), e.latLng.getLng());
        }
    });

    // 5. 다크 모드
    controls.dark?.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        const isDark = document.body.classList.contains('dark-mode');
        controls.dark.innerHTML = isDark ? "☀️ 주간" : "🌙 야간";
        controls.dark.classList.toggle('active');
    });

    // 6. 반경 표시 (500m, 1km)
    controls.radius?.addEventListener('click', () => {
        if (controls.radius.classList.contains('active')) {
            currentCircles.forEach(c => c.setMap(null));
            currentCircles = [];
            controls.radius.classList.remove('active');
        } else {
            const center = map.getCenter();
            [500, 1000].forEach((r, i) => {
                const color = i === 0 ? '#337cf4' : '#ff5050';
                const circle = new kakao.maps.Circle({
                    center, radius: r, strokeWeight: 1, strokeColor: color, strokeOpacity: 0.5, fillColor: color, fillOpacity: 0.1
                });
                circle.setMap(map);
                currentCircles.push(circle);
            });
            controls.radius.classList.add('active');
        }
    });

    // 7. 내 위치 트래킹
    controls.track?.addEventListener('click', () => {
        if (controls.track.classList.contains('active')) {
            if (watchId) navigator.geolocation.clearWatch(watchId);
            watchId = null;
            controls.track.classList.remove('active');
            controls.track.innerHTML = "📍 고정";
        } else {
            if (!navigator.geolocation) return alert("GPS 사용 불가");

            controls.track.classList.add('active');
            controls.track.innerHTML = "🔒 해제";
            watchId = navigator.geolocation.watchPosition(
                (pos) => map.panTo(new kakao.maps.LatLng(pos.coords.latitude, pos.coords.longitude)),
                null,
                { enableHighAccuracy: true }
            );
        }
    });

    // 8. 긴급 문자 신고
    controls.sms?.addEventListener('click', () => {
        const center = map.getCenter();
        if(confirm("🚨 긴급 구조 문자를 보내시겠습니까?")) {
            const msg = `[구조요청] 위급상황! 위치: 위도${center.getLat().toFixed(4)}, 경도${center.getLng().toFixed(4)}`;
            window.location.href = `sms:119?body=${encodeURIComponent(msg)}`;
        }
    });
}