// 시설물 마커 생성·클러스터링과 시설 기반 안전 점수 계산
import { map, clusterer } from './map-core.js';
import { updateSidebar, showToast } from './map-ui.js';
import { escapeHtml } from '../common/escape.js';
import { fetchRetry } from '../common/fetch-retry.js';

let markerImages: Record<string, any> = {};
let currentOverlay: any = null;

export function setupMarkerImages() {
    if (typeof kakao === 'undefined') return;

    const size = new kakao.maps.Size(100, 100);
    const options = { offset: new kakao.maps.Point(50, 90) };

    // 시설 유형별 이미지
    markerImages.fire = new kakao.maps.MarkerImage('/img/markers/marker_fire.png', size, options);
    markerImages.police = new kakao.maps.MarkerImage('/img/markers/marker_police.png', size, options);
    markerImages.hospital = new kakao.maps.MarkerImage('/img/markers/marker_hospital.png', size, options);
    markerImages.shelter = new kakao.maps.MarkerImage('/img/markers/marker_shelter.png', size, options);
    markerImages.default = new kakao.maps.MarkerImage('/img/markers/marker_default.png', size, options);
    markerImages.resting = new kakao.maps.MarkerImage('/img/markers/marker_resting.png', size, options);

    // 대피소 수용인원 등급별 이미지
    markerImages.shelter_high = new kakao.maps.MarkerImage('/img/markers/marker_shelter_high.png', size, options);
    markerImages.shelter_mid = new kakao.maps.MarkerImage('/img/markers/marker_shelter_mid.png', size, options);
    markerImages.shelter_low = new kakao.maps.MarkerImage('/img/markers/marker_shelter_low.png', size, options);
}

export async function updateMarkers() {
    if (Object.keys(markerImages).length === 0) setupMarkerImages();

    const bounds = map.getBounds();
    const sw = bounds.getSouthWest();
    const ne = bounds.getNorthEast();
    const queryParams = `swLat=${sw.getLat()}&swLng=${sw.getLng()}&neLat=${ne.getLat()}&neLng=${ne.getLng()}`;
    const facilityTypes = getCheckedTypes();

    clusterer.clear();
    if(currentOverlay) currentOverlay.setMap(null);

    // 선택된 필터가 없으면 마커만 비우고 종료 (안전점수는 지점 기준이라 별개)
    if (facilityTypes.length === 0) {
        return;
    }

    // 너무 줌아웃하면 시설이 수만 개라 마커를 그리지 않는다 (확대 유도)
    if (map.getLevel() > 8) {
        showToast("지도를 확대하면 주변 시설이 표시됩니다.");
        return;
    }

    try {
        const requests = facilityTypes.map(type =>
            fetch(`/api/facilities?type=${type}&${queryParams}`).then(res => res.json())
        );
        const results = await Promise.all(requests);
        const allFacilities = results.flat();

        if (allFacilities.length > 0) {
            drawMarkers(allFacilities);
        }

    } catch (error) {
        console.error('Facility data load failed:', error);
    }
}

function drawMarkers(facilities) {
    const newMarkers = facilities.map(facility => {
        const position = new kakao.maps.LatLng(facility.latitude, facility.longitude);
        const image = getMarkerImage(facility) || markerImages.default;
        const marker = new kakao.maps.Marker({ position, image });

        kakao.maps.event.addListener(marker, 'click', () => showCustomOverlay(marker, facility));
        return marker;
    });
    clusterer.addMarkers(newMarkers);
}

function getMarkerImage(facility) {
    const type = (facility.type || "").toLowerCase();
    const status = facility.operatingStatus;
    const capacity = facility.maxCapacity || 0;

    if (status && (status.includes('휴업') || status.includes('일시중지'))) return markerImages.resting;
    if (status && (status.includes('폐업') || status.includes('취소'))) return markerImages.default;

    if (type === 'police') return markerImages.police;
    if (type === 'fire') return markerImages.fire;
    if (type === 'hospital') return markerImages.hospital;

    if (type === 'shelter') {
        if (capacity >= 1000) return markerImages.shelter_high;
        if (capacity >= 300) return markerImages.shelter_mid;
        return markerImages.shelter_low;
    }

    return markerImages.default;
}

function showCustomOverlay(marker, facility) {
    if (currentOverlay) currentOverlay.setMap(null);

    const opStatus = facility.operatingStatus || "";
    const isClosed = opStatus.match(/휴업|폐업|취소/);
    const statusText = isClosed ? opStatus : "운영중";
    const statusColor = isClosed ? "#d9534f" : "#28a745";

    const content = document.createElement('div');
    content.className = 'kb-custom-overlay';

    let capacityInfo = "";
    if(facility.type === 'shelter' && facility.maxCapacity) {
        capacityInfo = `<div style="font-size:11px; color:#666; margin-bottom:5px;">수용: ${facility.maxCapacity}명</div>`;
    }

    content.innerHTML = `
        <div class="overlay-title">${escapeHtml(facility.name)}</div>
        <div class="overlay-status" style="color:${statusColor}">● ${escapeHtml(statusText)}</div>
        ${capacityInfo}
        <button class="overlay-btn">자세히 보기 ></button>
    `;

    content.querySelector('.overlay-btn').addEventListener('click', () => {
        handleMarkerClick(facility.id);
        if(currentOverlay) currentOverlay.setMap(null);
    });

    currentOverlay = new kakao.maps.CustomOverlay({
        content: content,
        map: map,
        position: marker.getPosition(),
        yAnchor: 1.35,
        zIndex: 100,
        clickable: true
    });

    kakao.maps.event.addListener(map, 'click', () => {
        if(currentOverlay) currentOverlay.setMap(null);
    });
}

async function handleMarkerClick(facilityId) {
    if (!facilityId) return;
    try {
        const res = await fetch(`/api/facilities/detail/${facilityId}`);
        if (!res.ok) throw new Error("API error");
        const data = await res.json();
        updateSidebar(data);
    } catch (error) {
        console.error(error);
    }
}

function getCheckedTypes() {
    return Array.from(document.querySelectorAll('.kb-target-checkbox:checked'))
        .map(cb => cb.getAttribute('data-type'));
}

export function setupMapEventListeners() {
    const reSearchBtn = document.getElementById('btn-re-search');

    const showReSearchBtn = () => { if(reSearchBtn) reSearchBtn.style.display = 'block'; };

    kakao.maps.event.addListener(map, 'dragend', showReSearchBtn);
    kakao.maps.event.addListener(map, 'zoom_changed', showReSearchBtn);

    reSearchBtn?.addEventListener('click', function() {
        updateMarkers();
        this.style.display = 'none';
    });

    document.querySelectorAll('.kb-target-checkbox').forEach(cb => {
        cb.addEventListener('change', () => updateMarkers());
    });

    // 대피 접근성: 지점 기준(내 위치 → 실패 시 지도 중심) 1회 평가. 줌·필터로 바뀌지 않음.
    initSafetyScore();
    document.getElementById('ss-here-btn')?.addEventListener('click', () => {
        const c = map.getCenter();
        evaluateSafetyScore(c.getLat(), c.getLng(), false);
    });
    document.getElementById('ss-shelter-btn')?.addEventListener('click', () => {
        if (nearestShelterPos) {
            map.setCenter(new kakao.maps.LatLng(nearestShelterPos.lat, nearestShelterPos.lng));
            map.setLevel(3);
        }
    });

    updateMarkers();
}

// ── 대피 접근성 점수 (서버 /api/safety-score · 지점 기준) ──────────────
// 구버전: 화면에 보이는 시설 개수 합산 → 줌·필터로 점수가 흔들림.
// 신버전: 내 위치(또는 지도 중심) 한 지점을 서버가 거리·위험구역 기반으로 평가.
const GRADE_COLOR: Record<string, string> = {
    "우수": "#28a745", "양호": "#2563eb", "보통": "#ffc107", "주의": "#d9534f",
};
let nearestShelterPos: { lat: number; lng: number } | null = null;

function initSafetyScore() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            pos => evaluateSafetyScore(pos.coords.latitude, pos.coords.longitude, true),
            () => { const c = map.getCenter(); evaluateSafetyScore(c.getLat(), c.getLng(), false); },
            { timeout: 5000, maximumAge: 60000 }
        );
    } else {
        const c = map.getCenter();
        evaluateSafetyScore(c.getLat(), c.getLng(), false);
    }
}

async function evaluateSafetyScore(lat, lng, fromMyLocation) {
    const panel = document.getElementById('safety-score-panel');
    if (!panel) return;
    try {
        const res = await fetchRetry(`/api/safety-score?lat=${lat}&lng=${lng}`);
        renderSafetyScore(await res.json(), fromMyLocation);
    } catch (e) {
        console.error('대피 접근성 점수 조회 실패:', e);
        panel.style.display = 'none';
    }
}

function renderSafetyScore(data, fromMyLocation) {
    const panel = document.getElementById('safety-score-panel');
    const valEl = document.getElementById('safety-score-val');
    const gradeEl = document.getElementById('safety-grade');
    if (!panel || !valEl || !gradeEl) return;

    const color = GRADE_COLOR[data.grade] || '#999';

    // 점수 링 게이지(0~100 비율만큼 채움) + 가운데 숫자
    const ring = document.getElementById('safety-score-ring');
    if (ring) {
        const R = 19, C = 2 * Math.PI * R;
        const pct = Math.max(0, Math.min(100, data.score)) / 100;
        ring.style.strokeDasharray = String(C);
        ring.style.strokeDashoffset = String(C * (1 - pct));
        ring.style.stroke = color;
    }
    valEl.innerText = String(data.score);
    valEl.setAttribute('aria-label', `대피 접근성 ${data.score}점, ${data.grade}`);

    // 등급 칩(연한 색 배경)
    gradeEl.innerText = data.grade;
    gradeEl.style.color = color;
    gradeEl.style.backgroundColor = color + '1f';

    // 기준 라벨(내 위치 / 지도 중심)
    const ctxEl = document.getElementById('ss-context');
    if (ctxEl) ctxEl.innerText = fromMyLocation ? '내 위치 기준' : '지도 중심 기준';

    // 행동 헤드라인(히어로): 가장 가까운 운영 대피소 + 도보 추정시간
    const headlineEl = document.getElementById('ss-headline');
    const shelterBtn = document.getElementById('ss-shelter-btn');
    if (data.nearestShelter) {
        const s = data.nearestShelter;
        const dist = s.distanceM < 1000 ? `${s.distanceM}m` : `${(s.distanceM / 1000).toFixed(1)}km`;
        if (headlineEl) headlineEl.innerText = `대피소 ${dist} · 도보 ${s.walkMinutes}분`;
        nearestShelterPos = { lat: s.lat, lng: s.lng };
        if (shelterBtn) shelterBtn.style.display = 'block';
    } else {
        if (headlineEl) headlineEl.innerText = '5km 내 운영 대피소 없음';
        nearestShelterPos = null;
        if (shelterBtn) shelterBtn.style.display = 'none';
    }

    // 현재 위험구역 노출 배너
    const hazardEl = document.getElementById('ss-hazard');
    const hazardNameEl = document.getElementById('ss-hazard-name');
    if (hazardEl) hazardEl.style.display = data.hazardActive ? 'block' : 'none';
    if (hazardNameEl) hazardNameEl.innerText = data.hazardName || '';

    // "왜 이 점수" 근거
    const breakdownEl = document.getElementById('ss-breakdown');
    if (breakdownEl) {
        breakdownEl.innerHTML = '';
        (data.breakdown || []).forEach(line => {
            const li = document.createElement('li');
            li.innerText = line;
            breakdownEl.appendChild(li);
        });
    }

    panel.style.display = 'flex';
}