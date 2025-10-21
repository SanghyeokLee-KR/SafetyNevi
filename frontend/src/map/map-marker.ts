// 시설물 마커 생성·클러스터링과 시설 기반 안전 점수 계산
import { map, clusterer } from './map-core.js';
import { updateSidebar, showToast } from './map-ui.js';

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

    // 선택된 필터가 없으면 안전 점수만 초기화 후 종료
    if (facilityTypes.length === 0) {
        if(window.calculateSafetyScore) window.calculateSafetyScore([]);
        return;
    }

    // 너무 줌아웃하면 시설이 수만 개라 마커를 그리지 않는다 (확대 유도)
    if (map.getLevel() > 8) {
        showToast("지도를 확대하면 주변 시설이 표시됩니다.");
        if(window.calculateSafetyScore) window.calculateSafetyScore([]);
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

        if(window.calculateSafetyScore) window.calculateSafetyScore(allFacilities);

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
        <div class="overlay-title">${facility.name}</div>
        <div class="overlay-status" style="color:${statusColor}">● ${statusText}</div>
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

    window.calculateSafetyScore = calculateSafetyScore;
    updateMarkers();
}

function calculateSafetyScore(facilities) {
    const panel = document.getElementById('safety-score-panel');
    const valEl = document.getElementById('safety-score-val');
    const gradeEl = document.getElementById('safety-grade');
    if (!panel) return;

    // 분석할 시설이 없으면(검색 전·줌아웃·필터 해제) 패널을 숨긴다 — '취약 0'이 경고처럼 뜨는 것 방지
    if (!facilities || facilities.length === 0) {
        panel.style.display = 'none';
        return;
    }

    let score = 0;
    facilities.forEach(f => {
        const t = (f.type || "").toLowerCase();
        if (t === 'police' || t === 'fire') score += 10;
        else if (t === 'hospital') score += 5;
        else if (t === 'shelter') score += 2;
    });
    score = Math.min(score, 99);

    if(valEl) valEl.innerText = String(score);

    if(gradeEl) {
        let color, text;
        if (score >= 80) { text = "매우 안전"; color = "#28a745"; }
        else if (score >= 50) { text = "보통"; color = "#ffc107"; }
        else { text = "취약"; color = "#d9534f"; }

        gradeEl.innerText = text;
        gradeEl.style.color = color;
        if(valEl) valEl.style.backgroundColor = color;
    }
    panel.style.display = 'flex';
}