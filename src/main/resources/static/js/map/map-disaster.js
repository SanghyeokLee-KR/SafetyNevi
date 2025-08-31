/**
 * 재난 구역 시각화 및 알림 관리
 */
import { map } from './map-core.js';

let disasterMarkerImages = {};
let currentDisasterZones = [];
let sigunguGeoJson = null;
let isModalShowing = false;
let processedDisasterIds = [];

// 재난 명칭 매핑 (영문/한글 -> 표시명)
const disasterNames = {
    'fire': '🔥 화재/산불', 'missile': '🚀 미사일/공습', 'lightning': '⚡ 낙뢰',
    'quake': '🌋 지진', 'typhoon': '🌀 태풍', 'heatwave': '☀️ 폭염',
    'heavyrain': '🌧️ 호우/장마', 'tsunami': '🌊 해일', 'flood': '🌊 홍수',
    'snow': '❄️ 대설', 'coldwave': '🥶 한파', 'dust': '🌫️ 황사/미세먼지'
};

// 재난 마커 이미지 설정
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

// 재난 구역 업데이트
export async function updateDisasterZones() {
    try {
        const response = await fetch('/api/disaster-zones');
        if (!response.ok) throw new Error("API error");
        const zones = await response.json();

        // 기존 구역 초기화
        currentDisasterZones.forEach(graphic => graphic.setMap(null));
        currentDisasterZones = [];

        // 신규 재난 알림 표시
        showDisasterAlert(zones);

        // 지도에 구역 그리기
        for (const zone of zones) {
            const style = getDisasterStyle(zone.disasterType);
            const markerImg = getDisasterMarkerImage(zone.disasterType);

            // 1. 원형 구역 (좌표 및 반경)
            if (zone.radius > 0 && zone.latitude && zone.longitude) {
                drawCircleZone(zone, style, markerImg);
            }

            // 2. 행정구역 폴리곤
            if (zone.areaName) {
                await drawPolygonZone(zone.areaName, style, markerImg);
            }
        }
    } catch (e) {
        console.error("Disaster zone update failed:", e);
    }
}

// 재난 알림 모달 처리
function showDisasterAlert(zones) {
    if (zones.length === 0) return;

    const newDisaster = zones.find(zone => !processedDisasterIds.includes(zone.id));
    if (newDisaster && !isModalShowing) {
        isModalShowing = true;
        processedDisasterIds.push(newDisaster.id);

        const modal = document.getElementById('disaster-modal');
        const msgEl = document.getElementById('disaster-modal-message');

        let typeName = disasterNames[newDisaster.disasterType] || "⚠️ 재난 경보";
        msgEl.innerHTML = `🚨 긴급: '${newDisaster.areaName || "인근"}' 지역 ${typeName}`;

        modal.classList.add('show');

        // 알림 클릭 시 해당 위치로 이동
        modal.onclick = () => {
            if (newDisaster.latitude && newDisaster.longitude) {
                map.setLevel(7);
                map.panTo(new kakao.maps.LatLng(newDisaster.latitude, newDisaster.longitude));
            }
        };

        setTimeout(() => {
            modal.classList.remove('show');
            isModalShowing = false;
        }, 5000);
    }
}

// 재난 유형별 스타일(색상) 반환
function getDisasterStyle(type) {
    const t = (type || "").toLowerCase();
    if (t.match(/fire|missile|heat|화재/)) return { fill: '#FF0000', stroke: '#FF0000' };
    if (t.match(/water|rain|flood|tsunami|호우/)) return { fill: '#0000FF', stroke: '#0000FF' };
    if (t.match(/quake|지진/)) return { fill: '#8B4513', stroke: '#D2691E' };
    if (t.match(/snow|cold|대설/)) return { fill: '#B0C4DE', stroke: '#778899' };
    if (t.match(/dust|황사/)) return { fill: '#FFD700', stroke: '#DAA520' };
    return { fill: '#FFA500', stroke: '#FF8C00' };
}

// 재난 마커 이미지 매칭
function getDisasterMarkerImage(type) {
    if (!type) return disasterMarkerImages.default;
    const t = type.toLowerCase();

    // 단순화된 매칭 로직
    for (const key in disasterMarkerImages) {
        if (t.includes(key)) return disasterMarkerImages[key];
    }
    return disasterMarkerImages.default;
}

// 원형 구역 그리기
function drawCircleZone(zone, style, image) {
    const circle = new kakao.maps.Circle({
        center: new kakao.maps.LatLng(zone.latitude, zone.longitude),
        radius: zone.radius,
        strokeWeight: 2,
        strokeColor: style.stroke,
        strokeOpacity: 0.8,
        fillColor: style.fill,
        fillOpacity: 0.4
    });
    circle.setMap(map);
    currentDisasterZones.push(circle);
    drawMarker(zone.latitude, zone.longitude, image);
}

// 마커 그리기 헬퍼
function drawMarker(lat, lng, image) {
    const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(lat, lng),
        image: image,
        zIndex: 10
    });
    marker.setMap(map);
    currentDisasterZones.push(marker);
}

// 행정구역 폴리곤 그리기
async function drawPolygonZone(areaName, style, markerImg) {
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
                    path: path,
                    strokeWeight: 2,
                    strokeColor: style.stroke,
                    strokeOpacity: 0.8,
                    fillColor: style.fill,
                    fillOpacity: 0.35
                });
                polygon.setMap(map);
                currentDisasterZones.push(polygon);

                latSum += path[0].getLat();
                lngSum += path[0].getLng();
                count++;
            };

            if (type === "Polygon") {
                drawPath(coords[0]);
            } else if (type === "MultiPolygon") {
                coords.forEach(c => drawPath(c[0]));
            }
        });

        // 중심점에 마커 표시
        if (count > 0) {
            drawMarker(latSum / count, lngSum / count, markerImg);
        }
    } catch (e) {
        console.error("Polygon drawing failed:", e);
    }
}

// GeoJSON에서 행정구역 Feature 검색
function findGeoJsonFeatures(areaName) {
    const nameParts = areaName.split(',').map(s => s.trim());
    const primary = nameParts[0];

    // 시도 코드 매핑
    const sidoMap = { '서울':'11', '부산':'21', '대구':'22', '인천':'23', '광주':'24', '대전':'25', '울산':'26', '세종':'29', '경기':'31', '강원':'32', '충북':'33', '충남':'34', '전북':'35', '전남':'36', '경북':'37', '경남':'38', '제주':'39' };
    let codePrefix = null;

    for (const [key, val] of Object.entries(sidoMap)) {
        if (primary.includes(key)) { codePrefix = val; break; }
    }

    if (codePrefix) {
        const sidoFeatures = sigunguGeoJson.features.filter(f => f.properties.code.startsWith(codePrefix));

        if (nameParts.length > 1) {
            // 상세 시군구 필터링
            return sidoFeatures.filter(f => nameParts.slice(1).some(d => f.properties.name.includes(d)));
        }
        // 시도 전체 또는 시군구 검색
        const districts = sidoFeatures.filter(f => primary.includes(f.properties.name));
        return districts.length > 0 ? districts : sidoFeatures;
    }

    return sigunguGeoJson.features.filter(f => areaName.includes(f.properties.name));
}