// 재난 구역을 지도에 그리고 WebSocket으로 생성/삭제를 실시간 반영
import { map } from './map-core.js';
import { prependDisasterMessage } from './map-disaster-feed.js';
import { getUserLocation } from './map-weather.js';

let disasterMarkerImages: Record<string, any> = {};
let zoneGraphics = new Map<any, any[]>();   // 재난 id -> 해당 구역의 그래픽(원/폴리곤/마커) 배열
let sigunguGeoJson: any = null;
let isModalShowing = false;
let alertedIds = new Set();     // 경보 모달을 이미 띄운 재난 id
let activeZones = new Map<any, any>();   // 재난 id -> zone 데이터(배너 거리 계산용)

const disasterNames: Record<string, string> = {
    'fire': '🔥 화재/산불', 'missile': '🚀 미사일/공습', 'lightning': '⚡ 낙뢰',
    'quake': '🌋 지진', 'typhoon': '🌀 태풍', 'heatwave': '☀️ 폭염',
    'heavyrain': '🌧️ 호우/장마', 'tsunami': '🌊 해일', 'flood': '🌊 홍수',
    'snow': '❄️ 대설', 'coldwave': '🥶 한파', 'dust': '🌫️ 황사/미세먼지'
};

export function setupDisasterMarkerImages() {
    const size = new kakao.maps.Size(100, 100);
    const options = { offset: new kakao.maps.Point(50, 90) };
    const path = '/img/disaster/';

    const keys = ['fire', 'missile', 'lightning', 'quake', 'typhoon', 'heatwave',
        'heavyrain', 'flood', 'tsunami', 'snow', 'coldwave', 'dust'];

    keys.forEach(key => {
        disasterMarkerImages[key] = new kakao.maps.MarkerImage(path + key + '.png', size, options);
    });
    disasterMarkerImages.default = new kakao.maps.MarkerImage(path + 'etc.png', size, options);
}

// 최초 진입 시 현재 활성 재난 구역을 한 번에 그린다 (이후 갱신은 소켓이 담당)
export async function loadDisasterZones() {
    try {
        const response = await fetch('/api/disaster-zones');
        if (!response.ok) throw new Error("API error");
        const zones = await response.json();

        zoneGraphics.forEach(arr => arr.forEach(g => g.setMap(null)));
        zoneGraphics.clear();
        activeZones.clear();

        for (const zone of zones) {
            await drawZone(zone, false); // 초기 로드는 경보 모달 없이
        }
    } catch (e) {
        console.error("재난 구역 로드 실패:", e);
    }
}

// 폴링 대신 소켓으로 재난 생성/삭제 push를 받는다
export function connectDisasterSocket() {
    const socket = new SockJS('/ws');
    const client = Stomp.over(socket);
    client.debug = null;

    client.connect({}, () => {
        client.subscribe('/topic/disaster/new', (msg) => drawZone(JSON.parse(msg.body), true));
        client.subscribe('/topic/disaster/delete', (msg) => removeZone(JSON.parse(msg.body)));
        client.subscribe('/topic/disaster-message', (msg) => prependDisasterMessage(JSON.parse(msg.body)));
    });

    // 사이드바 피드에서 지역 클릭 → 지도를 그 지역으로 이동
    document.addEventListener('feed:focus-area', (e: any) => focusOnArea(e.detail));

    // 비상 대피 배너의 '대피 경로' 버튼 → 안전 대피소 길찾기 (map-route.ts가 evac:start 를 받아 처리)
    const evacBtn = document.getElementById('kb-evac-go');
    if (evacBtn) evacBtn.addEventListener('click', () => document.dispatchEvent(new CustomEvent('evac:start')));

    // 내 위치가 (뒤늦게) 잡히면 재난과의 거리를 다시 계산해 배너 갱신
    document.addEventListener('location:updated', updateEvacBanner);
}

// 행정구역명으로 지도를 그 지역 중심으로 이동 (피드 카드 클릭용)
async function focusOnArea(areaName: string) {
    if (!areaName || !map) return;
    try {
        if (!sigunguGeoJson) {
            const res = await fetch('/geojson/skorea-municipalities-2018-geo.json');
            if (res.ok) sigunguGeoJson = await res.json(); else return;
        }
        const features = findGeoJsonFeatures(areaName);
        if (features.length === 0) return;

        let latSum = 0, lngSum = 0, count = 0;
        features.forEach(f => {
            const rings = f.geometry.type === 'Polygon'
                ? [f.geometry.coordinates[0]]
                : f.geometry.coordinates.map((c: any) => c[0]);
            rings.forEach((ring: any) => { latSum += ring[0][1]; lngSum += ring[0][0]; count++; });
        });
        if (count === 0) return;

        map.setLevel(9);
        map.panTo(new kakao.maps.LatLng(latSum / count, lngSum / count));
    } catch (e) {
        console.error('지역 포커스 실패:', e);
    }
}

// 재난 하나를 그리고 id로 그래픽을 보관 (이미 그려져 있으면 무시)
async function drawZone(zone, alert) {
    if (!zone || zoneGraphics.has(zone.id)) return;

    const graphics = [];
    const style = getDisasterStyle(zone.disasterType);
    const markerImg = getDisasterMarkerImage(zone.disasterType);

    if (zone.radius > 0 && zone.latitude && zone.longitude) {
        drawCircleZone(zone, style, markerImg, graphics);
    }
    if (zone.areaName) {
        await drawPolygonZone(zone.areaName, style, markerImg, graphics);
    }

    zoneGraphics.set(zone.id, graphics);
    activeZones.set(zone.id, zone);
    updateEvacBanner();
    if (alert) showDisasterAlert(zone);
}

// 특정 재난의 그래픽만 제거 (전체 재그리기 없이 변경분만)
function removeZone(id) {
    const arr = zoneGraphics.get(id);
    if (arr) {
        arr.forEach(g => g.setMap(null));
        zoneGraphics.delete(id);
        activeZones.delete(id);
        updateEvacBanner();
    }
}

const NEAR_KM = 10;   // 위험구역 경계로부터 이 거리 안이면 '인근'으로 본다

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// disasterNames("🔥 화재/산불")에서 이모지 떼고 한글 라벨만
function typeLabel(type: string): string {
    const full = disasterNames[(type || '').toLowerCase()];
    return full ? full.split(' ').slice(1).join(' ') : '재난';
}

// 비상 배너: 활성 재난이 '실제로 내 근처일 때만' 띄운다. 멀리 있는 재난을 '인근'이라 거짓말하지 않는다.
function updateEvacBanner() {
    const banner = document.getElementById('kb-evac-banner') as HTMLElement | null;
    if (!banner) return;
    const textEl = document.getElementById('kb-evac-text');

    if (activeZones.size === 0) { banner.hidden = true; return; }

    const loc = getUserLocation();

    // 좌표 있는 재난들 중, 내 위치에서 위험구역 '경계'까지 가장 가까운 것
    let nearest: any = null;
    if (loc) {
        activeZones.forEach((zone) => {
            if (zone.latitude == null || zone.longitude == null) return;
            const centerKm = haversineKm(loc.lat, loc.lon, zone.latitude, zone.longitude);
            const radiusKm = (zone.radius || 0) / 1000;
            const edgeKm = Math.max(0, centerKm - radiusKm);   // 위험구역 안이면 0
            if (!nearest || edgeKm < nearest.edgeKm) nearest = { zone, edgeKm };
        });
    }

    let text = '';
    if (!loc || !nearest) {
        // 위치를 모르면 '인근'이라 단정할 수 없으니 중립 안내
        text = '재난이 발생했습니다 — 대피 경로를 확인하세요';
    } else if (nearest.edgeKm === 0) {
        text = `현재 위치가 ${typeLabel(nearest.zone.disasterType)} 영향권 — 지금 대피하세요`;
    } else if (nearest.edgeKm <= NEAR_KM) {
        const d = nearest.edgeKm < 1 ? Math.round(nearest.edgeKm * 1000) + 'm' : nearest.edgeKm.toFixed(1) + 'km';
        text = `인근 ${typeLabel(nearest.zone.disasterType)} 발생 · 약 ${d} — 대피 경로 확인`;
    } else {
        banner.hidden = true;   // 멀리 있는 재난은 배너로 안 띄움
        return;
    }

    if (textEl) textEl.textContent = text;
    banner.hidden = false;
}

function showDisasterAlert(zone) {
    if (!zone || alertedIds.has(zone.id) || isModalShowing) return;

    isModalShowing = true;
    alertedIds.add(zone.id);

    const modal = document.getElementById('disaster-modal');
    const msgEl = document.getElementById('disaster-modal-message');
    if (!modal || !msgEl) { isModalShowing = false; return; }

    const typeName = disasterNames[zone.disasterType] || "⚠️ 재난 경보";
    msgEl.innerHTML = `🚨 긴급: '${zone.areaName || "인근"}' 지역 ${typeName}`;
    modal.classList.add('show');

    modal.onclick = () => {
        if (zone.latitude && zone.longitude) {
            map.setLevel(7);
            map.panTo(new kakao.maps.LatLng(zone.latitude, zone.longitude));
        }
    };

    setTimeout(() => {
        modal.classList.remove('show');
        isModalShowing = false;
    }, 5000);
}

function getDisasterStyle(type) {
    const t = (type || "").toLowerCase();
    if (t.match(/fire|missile|heat|화재/)) return { fill: '#FF0000', stroke: '#FF0000' };
    if (t.match(/water|rain|flood|tsunami|호우/)) return { fill: '#0000FF', stroke: '#0000FF' };
    if (t.match(/quake|지진/)) return { fill: '#8B4513', stroke: '#D2691E' };
    if (t.match(/snow|cold|대설/)) return { fill: '#B0C4DE', stroke: '#778899' };
    if (t.match(/dust|황사/)) return { fill: '#FFD700', stroke: '#DAA520' };
    return { fill: '#FFA500', stroke: '#FF8C00' };
}

function getDisasterMarkerImage(type) {
    if (!type) return disasterMarkerImages.default;
    const t = type.toLowerCase();
    for (const key in disasterMarkerImages) {
        if (t.includes(key)) return disasterMarkerImages[key];
    }
    return disasterMarkerImages.default;
}

function drawCircleZone(zone, style, image, graphics) {
    const circle = new kakao.maps.Circle({
        center: new kakao.maps.LatLng(zone.latitude, zone.longitude),
        radius: zone.radius,
        strokeWeight: 2, strokeColor: style.stroke, strokeOpacity: 0.8,
        fillColor: style.fill, fillOpacity: 0.4
    });
    circle.setMap(map);
    graphics.push(circle);
    drawMarker(zone.latitude, zone.longitude, image, graphics);
}

function drawMarker(lat, lng, image, graphics) {
    const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(lat, lng), image: image, zIndex: 10
    });
    marker.setMap(map);
    graphics.push(marker);
}

async function drawPolygonZone(areaName, style, markerImg, graphics) {
    try {
        if (!sigunguGeoJson) {
            const res = await fetch('/geojson/skorea-municipalities-2018-geo.json');
            if (res.ok) sigunguGeoJson = await res.json();
            else return;
        }

        const features = findGeoJsonFeatures(areaName);
        if (features.length === 0) return;

        let latSum = 0, lngSum = 0, count = 0;

        features.forEach(feature => {
            const coords = feature.geometry.coordinates;
            const type = feature.geometry.type;

            const drawPath = (polygonCoords) => {
                const path = polygonCoords.map(p => new kakao.maps.LatLng(p[1], p[0]));
                const polygon = new kakao.maps.Polygon({
                    path: path, strokeWeight: 2, strokeColor: style.stroke,
                    strokeOpacity: 0.8, fillColor: style.fill, fillOpacity: 0.35
                });
                polygon.setMap(map);
                graphics.push(polygon);

                latSum += path[0].getLat();
                lngSum += path[0].getLng();
                count++;
            };

            if (type === "Polygon") drawPath(coords[0]);
            else if (type === "MultiPolygon") coords.forEach(c => drawPath(c[0]));
        });

        if (count > 0) drawMarker(latSum / count, lngSum / count, markerImg, graphics);
    } catch (e) {
        console.error("폴리곤 그리기 실패:", e);
    }
}

function findGeoJsonFeatures(areaName) {
    const nameParts = areaName.split(',').map(s => s.trim());
    const primary = nameParts[0];

    const sidoMap = { '서울':'11', '부산':'21', '대구':'22', '인천':'23', '광주':'24', '대전':'25', '울산':'26', '세종':'29', '경기':'31', '강원':'32', '충북':'33', '충남':'34', '전북':'35', '전남':'36', '경북':'37', '경남':'38', '제주':'39' };
    let codePrefix = null;

    for (const [key, val] of Object.entries(sidoMap)) {
        if (primary.includes(key)) { codePrefix = val; break; }
    }

    if (codePrefix) {
        const sidoFeatures = sigunguGeoJson.features.filter(f => f.properties.code.startsWith(codePrefix));

        if (nameParts.length > 1) {
            return sidoFeatures.filter(f => nameParts.slice(1).some(d => f.properties.name.includes(d)));
        }
        const districts = sidoFeatures.filter(f => primary.includes(f.properties.name));
        return districts.length > 0 ? districts : sidoFeatures;
    }

    return sigunguGeoJson.features.filter(f => areaName.includes(f.properties.name));
}
