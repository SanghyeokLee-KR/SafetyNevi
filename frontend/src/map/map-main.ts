// 지도 페이지 진입점: 초기화 → UI → 기능 → 데이터 로드 순으로 부팅
import { initMap } from './map-core.js';
import { setupTabNavigation, setupCheckboxLogic, setupDetailViewEvents, setupGlobalUI } from './map-ui.js';
import { setupMarkerImages, setupMapEventListeners } from './map-marker.js';
import { loadCurrentLocationAndWeather } from './map-weather.js';
import { setupDisasterMarkerImages, loadDisasterZones, connectDisasterSocket } from './map-disaster.js';
import { setupSearchLogic } from './map-search.js';
import { setupRouteLogic } from './map-route.js';
import { setupMyPlaceLogic } from './map-myplace.js';
import { setupBoardLogic } from './map-board.js';

document.addEventListener('DOMContentLoaded', async () => {

    // 지도 엔진과 마커 리소스가 먼저 떠야 나머지가 동작한다
    try {
        initMap();
        setupMarkerImages();
        setupDisasterMarkerImages();
    } catch (e) {
        console.error("Map initialization failed:", e);
        return;
    }

    setupTabNavigation();
    setupCheckboxLogic();
    setupDetailViewEvents();
    setupGlobalUI();

    setupSearchLogic();
    setupRouteLogic();
    setupMyPlaceLogic();
    setupBoardLogic();

    setupMapEventListeners();
    loadCurrentLocationAndWeather();

    // 재난은 초기 1회 로드 후 소켓으로 push 수신 (폴링 대신)
    loadDisasterZones();
    connectDisasterSocket();
});