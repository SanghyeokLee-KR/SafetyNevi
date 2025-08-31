/**
 * 현재 위치 및 날씨 정보 관리
 */
import { map } from './map-core.js';
import { toggleLoading, showToast } from './map-ui.js';

// 현재 위치 로드 및 날씨 정보 요청
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

// 현재 위치 마커 표시 및 지도 이동
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

    // 부드러운 이동 처리
    map.setLevel(4, { animate: { duration: 800 } });
    setTimeout(() => {
        map.panTo(locPosition);
    }, 300);
}

// 날씨 및 주소 정보 API 호출
async function fetchWeatherAndAddress(lat, lon) {
    try {
        const response = await fetch(`/api/weather?lat=${lat}&lon=${lon}`);
        if (!response.ok) return;
        const weatherDto = await response.json();
        updateWeatherUI(weatherDto);
    } catch (error) { console.error(error); }
}

// 날씨 UI 업데이트
function updateWeatherUI(data) {
    const addrEl = document.querySelector('#current-address');
    if (addrEl) addrEl.innerText = data.address || "주소정보 없음";

    const tempEl = document.querySelector('#current-temp');
    if (tempEl) tempEl.innerText = data.temp ? `${data.temp}°` : '';

    const weatherStatusEl = document.querySelector('#weather-status');
    if (weatherStatusEl) weatherStatusEl.innerText = data.weatherStatus || "";

    const weatherIconEl = document.querySelector('#weather-icon');
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