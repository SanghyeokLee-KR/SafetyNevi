// 현재 위치 파악 후 해당 좌표의 날씨·주소 표시
import { map } from './map-core.js';
import { toggleLoading, showToast } from './map-ui.js';
import { fetchRetry } from '../common/fetch-retry.js';

// 내 위치(위경도). 재난 배너 등 다른 모듈이 '재난과의 거리' 계산에 쓴다. 못 찾으면 null 유지(기본 위치는 내 위치로 치지 않음).
let userLat: number | null = null;
let userLon: number | null = null;
export function getUserLocation(): { lat: number; lon: number } | null {
    return (userLat !== null && userLon !== null) ? { lat: userLat, lon: userLon } : null;
}

export function loadCurrentLocationAndWeather() {
    showToast("내 위치를 찾는 중입니다...");

    if (navigator.geolocation) {
        const options = {
            enableHighAccuracy: true,
            timeout: 7000,
            maximumAge: 0
        };
        navigator.geolocation.getCurrentPosition(successCallback, errorCallback, options);
    } else {
        errorCallback(new Error("GPS 미지원"));
    }
}

function successCallback(position) {
    const lat = position.coords.latitude;
    const lon = position.coords.longitude;
    userLat = lat;
    userLon = lon;
    document.dispatchEvent(new CustomEvent('location:updated')); // 배너가 재난과의 거리 다시 계산하도록
    const locPosition = new kakao.maps.LatLng(lat, lon);

    displayMarker(locPosition);
    fetchWeatherAndAddress(lat, lon);
    showToast("내 위치를 찾았습니다! 📍");
}

function errorCallback(error) {
    console.warn("위치 파악 실패:", error);
    showToast("위치를 찾을 수 없어 기본 위치로 이동합니다.", true);

    const defaultLat = 37.566826;
    const defaultLon = 126.9786567;
    const locPosition = new kakao.maps.LatLng(defaultLat, defaultLon);

    displayMarker(locPosition);
    fetchWeatherAndAddress(defaultLat, defaultLon);
}

function displayMarker(locPosition) {
    if (!map) return;

    const content = document.createElement('div');
    content.className = 'kb-radar-wrapper';
    content.innerHTML = `
        <div class="kb-radar-ring"></div>
        <div class="kb-radar-ring"></div>
        <div class="kb-radar-dot"></div>
    `;

    new kakao.maps.CustomOverlay({
        map: map,
        position: locPosition,
        content: content,
        yAnchor: 0.5
    });

    // 줌 애니메이션(800ms)이 끝날 즈음 panTo 해야 끊김 없이 이어진다
    map.setLevel(4, { animate: { duration: 800 } });
    setTimeout(() => {
        map.panTo(locPosition);
    }, 300);
}

async function fetchWeatherAndAddress(lat, lon) {
    try {
        const response = await fetchRetry(`/api/weather?lat=${lat}&lon=${lon}`);
        updateWeatherUI(await response.json());
    } catch (error) {
        console.error('날씨·주소 로드 실패:', error);
        // 끝까지 실패하면 가짜 로딩('위치 확인 중…')에 머물지 않도록 명확히 표시
        const addrEl = document.querySelector<HTMLElement>('#current-address');
        if (addrEl && addrEl.innerText.indexOf('확인 중') !== -1) addrEl.innerText = '위치 확인 실패 · 새로고침';
    }
}

function updateWeatherUI(data: any) {
    const addrEl = document.querySelector<HTMLElement>('#current-address');
    if (addrEl) addrEl.innerText = data.address || "주소정보 없음";

    const tempEl = document.querySelector<HTMLElement>('#current-temp');
    const temp = data.temp;
    if (tempEl) tempEl.innerText = (temp && temp !== 'N/A') ? `${temp}°` : '';

    const weatherStatusEl = document.querySelector<HTMLElement>('#weather-status');
    if (weatherStatusEl) weatherStatusEl.innerText = data.weatherStatus || "";

    const weatherIconEl = document.querySelector<HTMLImageElement>('#weather-icon');
    if (weatherIconEl && data.weatherStatus) {
        const status = data.weatherStatus;
        let iconSrc = 'default.png';
        if (status.includes('맑음')) iconSrc = 'sunny.png';
        else if (status.includes('구름')) iconSrc = 'cloudy.png';
        else if (status.includes('흐림')) iconSrc = 'overcast.png';
        else if (status.includes('비')) iconSrc = 'rain.png';
        else if (status.includes('눈')) iconSrc = 'snow.png';

        weatherIconEl.src = `/img/weather/${iconSrc}`;
        weatherIconEl.style.display = 'inline-block';
    }
}