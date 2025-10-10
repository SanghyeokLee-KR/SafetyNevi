/**
 * UI 인터랙션 및 공통 컴포넌트 관리 (Sidebar, Tab, Modal)
 */
import { updateMarkers } from './map-marker.js';
import { setRouteDestination } from './map-route.js';

// 탭 네비게이션 설정
export function setupTabNavigation() {
    const tabButtons = document.querySelectorAll('.kb-tab-button');
    const routeFinder = document.getElementById('kb-route-finder');
    const generalContent = document.getElementById('kb-general-content');
    const myPage = document.getElementById('kb-my-page');
    const detailContent = document.getElementById('kb-detail-content');

    if (!routeFinder || !generalContent || !myPage) return;

    tabButtons.forEach(button => {
        button.addEventListener('click', function () {
            const targetTab = this.getAttribute('data-tab');

            tabButtons.forEach(btn => btn.classList.remove('kb-active'));
            this.classList.add('kb-active');

            routeFinder.style.display = 'none';
            generalContent.style.display = 'none';
            myPage.style.display = 'none';
            if(detailContent) detailContent.style.display = 'none';

            if (targetTab === 'route') routeFinder.style.display = 'block';
            else if (targetTab === 'general') generalContent.style.display = 'block';
            else if (targetTab === 'my') myPage.style.display = 'block';
        });
    });

    const transportModes = document.querySelectorAll('.kb-mode-button');
    transportModes.forEach(button => {
        button.addEventListener('click', function () {
            transportModes.forEach(btn => btn.classList.remove('kb-active'));
            this.classList.add('kb-active');
        });
    });
}

// 탐색 필터 체크박스 동기화
export function setupCheckboxLogic() {
    const allCheckbox = document.getElementById('kb-explore-all');
    const targetCheckboxes = document.querySelectorAll('.kb-target-checkbox');
    if (allCheckbox) {
        allCheckbox.addEventListener('change', function (this: HTMLInputElement) {
            targetCheckboxes.forEach(checkbox => (checkbox as HTMLInputElement).checked = this.checked);
            updateMarkers();
        });
    }
}

// 상세화면 UI 이벤트
export function setupDetailViewEvents() {
    const backBtn = document.getElementById('btn-detail-back');
    if (backBtn) {
        backBtn.addEventListener('click', function() {
            const detailPanel = document.getElementById('kb-detail-content');
            const generalPanel = document.getElementById('kb-general-content');

            if(detailPanel) detailPanel.style.display = 'none';
            if(generalPanel) generalPanel.style.display = 'block';

            const tabs = document.querySelectorAll('.kb-tab-button');
            tabs.forEach(t => t.classList.remove('kb-active'));

            const generalTab = document.querySelector('.kb-tab-button[data-tab="general"]');
            if(generalTab) generalTab.classList.add('kb-active');
        });
    }

    const reportBtn = document.getElementById('btn-report-facility');
    if(reportBtn) {
        reportBtn.addEventListener('click', () => {
            const addr = document.getElementById('current-address').innerText;
            openReportModal('FACILITY', null, `현재 위치(${addr}) 정보 오류`, null);
        });
    }
}

// 신고 모달 열기
export function openReportModal(type, id, name, user = null) {
    const modal = document.getElementById('common-report-modal');
    const targetText = document.getElementById('report-target-text');
    const blockOption = document.getElementById('report-block-option');

    (document.getElementById('report-type') as HTMLInputElement).value = type;
    (document.getElementById('report-id') as HTMLInputElement).value = id ?? "";
    (document.getElementById('report-user') as HTMLInputElement).value = user || "";
    (document.getElementById('report-desc') as HTMLInputElement).value = "";
    (document.getElementById('chk-block-user') as HTMLInputElement).checked = false;

    if (type === 'FACILITY') {
        targetText.innerText = `대상: ${name} (시설 정보 오류 신고)`;
        blockOption.style.display = 'none';
    } else {
        targetText.innerHTML = `대상 게시글: <b>${name}</b><br>작성자: ${user}`;
        blockOption.style.display = 'block';
    }
    modal.style.display = 'block';
}
window.openReportModal = openReportModal;

// 신고 전송 요청
export async function submitReport() {
    const type = (document.getElementById('report-type') as HTMLInputElement).value;
    const id = (document.getElementById('report-id') as HTMLInputElement).value || null;
    const reason = (document.getElementById('report-reason') as HTMLInputElement).value;
    const desc = (document.getElementById('report-desc') as HTMLInputElement).value;
    const isBlock = (document.getElementById('chk-block-user') as HTMLInputElement).checked;
    const targetUser = (document.getElementById('report-user') as HTMLInputElement).value;

    if (!reason) {
        alert("신고 사유를 선택해주세요.");
        return;
    }

    toggleLoading(true, "신고 처리 중...");

    if (type === 'BOARD' && isBlock && targetUser) {
        let blockedUsers = JSON.parse(localStorage.getItem('safety_blocked_users')) || [];
        if (!blockedUsers.includes(targetUser)) {
            blockedUsers.push(targetUser);
            localStorage.setItem('safety_blocked_users', JSON.stringify(blockedUsers));
        }
    }

    try {
        const res = await fetch('/api/report', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                targetType: type,
                targetId: id === "" ? null : id,
                targetUser: targetUser,
                reason: reason,
                description: desc
            })
        });

        if (!res.ok) throw new Error("Report API failed");

        toggleLoading(false);
        document.getElementById('common-report-modal').style.display = 'none';

        if (isBlock) {
            showToast(`신고 접수 및 차단 완료`);
            if (window.reloadBoardData) window.reloadBoardData();
        } else {
            showToast("신고가 정상적으로 접수되었습니다.");
        }
    } catch (e) {
        console.error(e);
        toggleLoading(false);
        alert("신고 처리 중 오류가 발생했습니다.");
    }
}

// 사이드바 컨텐츠 업데이트
export function updateSidebar(data) {
    const wrap = document.querySelector('.kb-wrap');
    if (wrap) wrap.classList.remove('kb-sidebar-hidden');

    const filters = document.querySelector<HTMLElement>('.kb-map-filters');
    if(filters) filters.style.display = 'none';

    const detailPanel = document.getElementById('kb-detail-content');
    const generalPanel = document.getElementById('kb-general-content');
    const routePanel = document.getElementById('kb-route-finder');
    const myPanel = document.getElementById('kb-my-page');
    const dynamicArea = document.getElementById('detail-dynamic-area');

    document.getElementById('detail-name').innerText = data.name || "이름 없음";
    document.getElementById('detail-address').innerText = data.address || "주소 없음";
    document.getElementById('detail-tel').innerText = (data.phoneNumber || data.tel) ?? "전화 없음";

    const timeEl = document.getElementById('detail-time');
    if (timeEl) timeEl.innerText = data.time || "정보 없음";

    const statusEl = document.getElementById('detail-status');
    let rawStatus = data.operatingStatus || '정보없음';
    if (data.type === 'police' || data.type === 'fire') rawStatus = '영업';

    let statusText = rawStatus;
    statusEl.className = '';

    if (rawStatus.includes('영업') || rawStatus.includes('정상')) {
        statusText = '● 운영중';
        statusEl.classList.add('kb-status-active');
    } else {
        statusText = `● ${rawStatus}`;
        statusEl.classList.add('kb-status-unknown');
    }
    statusEl.innerText = statusText;

    let dynamicHtml = "";
    const makeItem = (icon, label, value) => {
        if (!value || value === 'null' || value === 0) return "";
        return `<div class="detail-item"><strong>${icon} ${label}</strong> <span>${value}</span></div>`;
    };

    if (data.type === 'police') dynamicHtml += makeItem('👮', '구분', data.gubun);
    else if (data.type === 'fire') dynamicHtml += makeItem('🚒', '유형', data.subType);
    else if (data.type === 'hospital') {
        dynamicHtml += makeItem('🏥', '종류', data.subType) + makeItem('🛏️', '병상', data.bedCount) + makeItem('👨‍⚕️', '의료진', data.staffCount);
    } else if (data.type === 'shelter') {
        dynamicHtml += makeItem('👥', '수용', data.maxCapacity) + makeItem('📐', '면적', data.areaM2) + makeItem('🛖', '구분', data.locationType);
    }

    if (dynamicArea) dynamicArea.innerHTML = dynamicHtml;

    // 로드뷰 및 신고 버튼
    const roadviewArea = document.getElementById('roadview-btn-area');
    if (roadviewArea) {
        roadviewArea.innerHTML = `
            <div style="display:flex; gap:5px;">
                <button id="btn-show-roadview" style="flex:1; padding:8px; border:1px solid #ddd; background:#f9f9f9; border-radius:5px; cursor:pointer;">👁️ 로드뷰 보기</button>
                <button id="btn-report-detail" style="flex:0.3; padding:8px; border:1px solid #ffcccc; background:#fff5f5; color:#d9534f; border-radius:5px; cursor:pointer;">🚨 신고</button>
            </div>`;

        document.getElementById('btn-show-roadview').onclick = () => openRoadview(data.latitude, data.longitude);
        document.getElementById('btn-report-detail').onclick = () => openReportModal('FACILITY', data.id ?? null, data.name, null);
    }

    // 즐겨찾기, 주소복사 버튼
    const oldActions = document.querySelector('.detail-actions');
    if(oldActions) oldActions.remove();

    const actionContainer = document.createElement('div');
    actionContainer.className = 'detail-actions';
    actionContainer.innerHTML = `
        <button class="btn-action" id="btn-add-fav"><span>⭐</span> 즐겨찾기</button>
        <button class="btn-action" id="btn-copy-addr"><span>📋</span> 주소복사</button>
    `;

    const routeBtn = document.getElementById('btn-find-route');
    if(routeBtn) routeBtn.parentNode.insertBefore(actionContainer, routeBtn);

    document.getElementById('btn-add-fav').onclick = async () => {
        const { addToFavorites } = await import('./map-myplace.js');
        addToFavorites(data.name, data.address, data.latitude, data.longitude);
    };

    document.getElementById('btn-copy-addr').onclick = () =>
        navigator.clipboard.writeText(data.address || "").then(() => showToast("주소가 복사되었습니다!"));

    if (routeBtn) {
        routeBtn.innerHTML = '🚗 길찾기 (바로 안내)';
        routeBtn.onclick = function() {
            if (data.latitude && data.longitude) {
                const routeTabBtn = document.querySelector<HTMLElement>('.kb-tab-button[data-tab="route"]');
                if(routeTabBtn) routeTabBtn.click();
                setRouteDestination(data.name, data.latitude, data.longitude);
            } else alert("위치 정보 없음");
        };
    }

    if (generalPanel) generalPanel.style.display = 'none';
    if (routePanel) routePanel.style.display = 'none';
    if (myPanel) myPanel.style.display = 'none';
    if (detailPanel) detailPanel.style.display = 'block';
}

function openRoadview(lat, lng) {
    const modal = document.getElementById('roadview-modal');
    const rvContainer = document.getElementById('roadview');
    const closeBtn = document.getElementById('roadview-close');

    modal.style.display = 'flex';
    const roadview = new kakao.maps.Roadview(rvContainer);
    const roadviewClient = new kakao.maps.RoadviewClient();
    const position = new kakao.maps.LatLng(lat, lng);

    roadviewClient.getNearestPanoId(position, 100, function(panoId) {
        if (panoId) roadview.setPanoId(panoId, position);
        else {
            alert("해당 지역의 로드뷰 데이터가 없습니다.");
            modal.style.display = 'none';
        }
    });

    closeBtn.onclick = () => { modal.style.display = 'none'; };
}
window.openRoadview = openRoadview;

export function toggleLoading(show, message = "처리 중...") {
    let overlay = document.querySelector<HTMLElement>('.kb-loading-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'kb-loading-overlay';
        overlay.innerHTML = `<div class="kb-spinner"></div><div class="kb-loading-text"></div>`;
        document.body.appendChild(overlay);
    }
    if (show) {
        (overlay.querySelector('.kb-loading-text') as HTMLElement).innerText = message;
        overlay.style.display = 'flex';
    } else {
        overlay.style.display = 'none';
    }
}

export function showToast(message, isError = false) {
    let toast = document.querySelector<HTMLElement>('.kb-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'kb-toast';
        document.body.appendChild(toast);
    }
    toast.innerText = message;
    toast.className = isError ? 'kb-toast show error' : 'kb-toast show';
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// 전역 UI 설정 (필터, 사이드바 토글)
export function setupGlobalUI() {
    const wrap = document.querySelector('.kb-wrap');
    const closeBtn = document.querySelector('.kb-menu-icon');
    const openBtn = document.getElementById('btn-sidebar-open');
    const filters = document.querySelector<HTMLElement>('.kb-map-filters');
    const filterBtns = document.querySelectorAll('.kb-filter-btn');

    function updateFilterVisibility() {
        if (wrap.classList.contains('kb-sidebar-hidden')) {
            if(filters) filters.style.display = 'flex';
        } else {
            if(filters) filters.style.display = 'none';
        }
    }
    updateFilterVisibility();

    if (closeBtn && wrap) {
        closeBtn.addEventListener('click', () => {
            wrap.classList.add('kb-sidebar-hidden');
            updateFilterVisibility();
        });
    }

    if (openBtn && wrap) {
        openBtn.addEventListener('click', () => {
            wrap.classList.remove('kb-sidebar-hidden');
            updateFilterVisibility();
        });
    }

    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const type = btn.getAttribute('data-type');
            if (type === 'all') {
                const isNowActive = !btn.classList.contains('active');
                filterBtns.forEach(b => isNowActive ? b.classList.add('active') : b.classList.remove('active'));

                const allCheckbox = document.getElementById('kb-explore-all') as HTMLInputElement;
                if(allCheckbox) {
                    allCheckbox.checked = isNowActive;
                    allCheckbox.dispatchEvent(new Event('change'));
                }
            } else {
                btn.classList.toggle('active');
                const isActive = btn.classList.contains('active');
                const targetCheckbox = document.querySelector<HTMLInputElement>(`.kb-target-checkbox[data-type="${type}"]`);
                if(targetCheckbox) {
                    targetCheckbox.checked = isActive;
                    targetCheckbox.dispatchEvent(new Event('change'));
                }
            }
        });
    });
}