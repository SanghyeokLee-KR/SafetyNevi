// 재난 구역을 지도에 그리고 WebSocket으로 생성/삭제를 실시간 반영
import { map } from './map-core.js';
import { prependDisasterMessage } from './map-disaster-feed.js';

let disasterMarkerImages: Record<string, any> = {};
let zoneGraphics = new Map<any, any[]>();   // 재난 id -> 해당 구역의 그래픽(원/폴리곤/마커) 배열
let sigunguGeoJson: any = null;
let isModalShowing = false;
let alertedIds = new Set();     // 경보 모달을 이미 띄운 재난 id

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
    if (alert) showDisasterAlert(zone);
}

// 특정 재난의 그래픽만 제거 (전체 재그리기 없이 변경분만)
function removeZone(id) {
    const arr = zoneGraphics.get(id);
    if (arr) {
        arr.forEach(g => g.setMap(null));
        zoneGraphics.delete(id);
    }
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
