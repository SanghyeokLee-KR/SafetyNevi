/**
 * 지도 애플리케이션 진입점 (Entry Point)
 * - 지도 초기화, UI 설정, 이벤트 바인딩, 초기 데이터 로드 수행
 */
import { initMap } from './map-core.js';
import { setupTabNavigation, setupCheckboxLogic, setupDetailViewEvents, setupGlobalUI } from './map-ui.js';
import { setupMarkerImages, setupMapEventListeners } from './map-marker.js';
import { loadCurrentLocationAndWeather } from './map-weather.js';
import { setupDisasterMarkerImages, updateDisasterZones } from './map-disaster.js';
import { setupSearchLogic } from './map-search.js';
import { setupRouteLogic } from './map-route.js';
import { setupMyPlaceLogic } from './map-myplace.js';
import { setupBoardLogic } from './map-board.js';

document.addEventListener('DOMContentLoaded', async () => {

    // 1. 지도 엔진 및 리소스 초기화
    try {
        initMap();
        setupMarkerImages();
        setupDisasterMarkerImages();
    } catch (e) {
        console.error("Map initialization failed:", e);
        return;
    }

    // 2. UI 및 이벤트 핸들러 설정
    setupTabNavigation();
    setupCheckboxLogic();
    setupDetailViewEvents();
    setupGlobalUI();

    // 3. 주요 기능 로직 바인딩
    setupSearchLogic();
    setupRouteLogic();
    setupMyPlaceLogic();
    setupBoardLogic();

    // 4. 지도 이벤트 리스너 및 초기 데이터 로드
    setupMapEventListeners();
    loadCurrentLocationAndWeather();

    // 5. 재난 데이터 주기적 갱신 (10초 간격)
    updateDisasterZones();
    setInterval(updateDisasterZones, 10000);
});