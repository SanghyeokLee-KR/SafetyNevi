// 시설물 키워드 검색과 최근 검색어(localStorage) 관리
import { map } from './map-core.js';
import { updateSidebar } from './map-ui.js';
import { escapeHtml } from '../common/escape.js';

export function setupSearchLogic() {
    const toggleBtn = document.getElementById('btn-search-toggle');
    const closeBtn = document.getElementById('btn-search-close') as HTMLElement;
    const searchPanel = document.getElementById('kb-search-panel');
    const searchInput = document.getElementById('kb-search-input') as HTMLInputElement;
    const searchExecBtn = document.getElementById('btn-search-exec') as HTMLElement;
    const resultList = document.getElementById('kb-search-results') as HTMLElement;
    const recentArea = document.getElementById('kb-recent-area') as HTMLElement;
    const recentClearBtn = document.getElementById('btn-recent-clear');

    if (!toggleBtn || !searchPanel) return;

    toggleBtn.addEventListener('click', () => {
        if (searchPanel.style.display === 'none') {
            searchPanel.style.display = 'block';
            searchInput.focus();
            showRecentSearches();
        } else {
            searchPanel.style.display = 'none';
        }
    });

    closeBtn.addEventListener('click', () => {
        searchPanel.style.display = 'none';
        resultList.classList.remove('show');
    });

    const executeSearch = async () => {
        const keyword = searchInput.value.trim();
        if (keyword.length < 2) {
            alert("검색어를 2글자 이상 입력하세요.");
            return;
        }

        saveKeyword(keyword);
        recentArea.style.display = 'none';

        try {
            const response = await fetch(`/api/facilities/search?keyword=${encodeURIComponent(keyword)}`);
            if (!response.ok) throw new Error("Search failed");
            const results = await response.json();
            renderResults(results, keyword);
        } catch (e) {
            console.error(e);
            alert("검색 중 오류가 발생했습니다.");
        }
    };

    searchExecBtn.addEventListener('click', executeSearch);
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            executeSearch();
        }
    });

    searchInput.addEventListener('focus', () => {
        if (searchInput.value === '') showRecentSearches();
    });

    if (recentClearBtn) {
        recentClearBtn.addEventListener('click', () => {
            localStorage.removeItem('safety_recent_search');
            showRecentSearches();
        });
    }

    function showRecentSearches() {
        const history = JSON.parse(localStorage.getItem('safety_recent_search') || '[]') || [];
        const listEl = document.getElementById('kb-recent-list') as HTMLElement;

        if (history.length === 0) {
            recentArea.style.display = 'none';
            return;
        }

        listEl.innerHTML = '';
        history.forEach((item) => {
            const li = document.createElement('li');
            li.className = 'kb-recent-item';
            li.innerHTML = `<span>🕒 ${escapeHtml(item)}</span> <span class="btn-recent-del">✕</span>`;

            li.addEventListener('click', (e) => {
                if((e.target as HTMLElement).classList.contains('btn-recent-del')) return;
                searchInput.value = item;
                executeSearch();
            });

            li.querySelector('.btn-recent-del')?.addEventListener('click', (e) => {
                e.stopPropagation();
                deleteKeyword(item);
            });
            listEl.appendChild(li);
        });

        resultList.classList.remove('show');
        recentArea.style.display = 'block';
    }

    // 키워드 저장 (중복 제거, 최대 5개)
    function saveKeyword(keyword) {
        let history = JSON.parse(localStorage.getItem('safety_recent_search') || '[]') || [];
        history = history.filter(k => k !== keyword);
        history.unshift(keyword);
        if (history.length > 5) history.pop();
        localStorage.setItem('safety_recent_search', JSON.stringify(history));
    }

    function deleteKeyword(keyword) {
        let history = JSON.parse(localStorage.getItem('safety_recent_search') || '[]') || [];
        history = history.filter(k => k !== keyword);
        localStorage.setItem('safety_recent_search', JSON.stringify(history));
        showRecentSearches();
    }

    function renderResults(data, keyword) {
        resultList.innerHTML = '';
        if (data.length === 0) {
            resultList.innerHTML = '<li style="padding:15px; text-align:center; color:#888;">검색 결과가 없습니다.</li>';
            resultList.classList.add('show');
            return;
        }
        data.forEach(item => {
            const li = document.createElement('li');
            li.className = 'kb-search-item';

            // 이름은 먼저 이스케이프한 뒤 키워드 하이라이트. 키워드의 정규식 메타문자도 이스케이프(정규식 인젝션 방지)
            const safeName = escapeHtml(item.name);
            const safeKeyword = escapeHtml(keyword).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(`(${safeKeyword})`, 'gi');
            const highlightedName = safeName.replace(regex, '<span class="highlight-text">$1</span>');

            const typeLabel = item.type === 'police' ? '경찰서' : item.type === 'fire' ? '소방서' : item.type === 'hospital' ? '병원' : '대피소';

            li.innerHTML = `
                <div class="search-item-info">
                    <div class="search-item-name">${highlightedName}</div>
                    <div class="search-item-address">${escapeHtml(item.address) || '주소 정보 없음'}</div>
                </div>
                <div class="search-item-category">${typeLabel}</div>
            `;

            li.addEventListener('click', async () => {
                if (item.latitude && item.longitude) {
                    const moveLatLon = new kakao.maps.LatLng(item.latitude, item.longitude);
                    map.setCenter(moveLatLon);
                    map.setLevel(3);
                }
                try {
                    const detailRes = await fetch(`/api/facilities/detail/${item.id}`);
                    if(detailRes.ok) {
                        const detailData = await detailRes.json();
                        updateSidebar(detailData);
                    }
                } catch(e) { console.error(e); }
            });
            resultList.appendChild(li);
        });
        resultList.classList.add('show');
    }
}