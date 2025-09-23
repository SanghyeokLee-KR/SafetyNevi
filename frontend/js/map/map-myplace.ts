/**
 * 내 장소(My Place) 및 가족 연락처 관리
 */
import { map } from './map-core.js';
import { showToast } from './map-ui.js';
import { showBoardOverlay } from './map-board.js';

const geocoder = new kakao.maps.services.Geocoder();
let myPlaces = [];
let myPlaceMarkers = [];

const MARKER_IMGS = {
    HOME: '/img/location/home.png',
    COMPANY: '/img/location/company.png',
    FAVORITE: '/img/location/favorite.png'
};

export function setupMyPlaceLogic() {
    loadMyPlaces();

    const btnHome = document.getElementById('btn-set-home');
    const btnCompany = document.getElementById('btn-set-company');
    if (btnHome) btnHome.addEventListener('click', () => openAddressSearch('HOME'));
    if (btnCompany) btnCompany.addEventListener('click', () => openAddressSearch('COMPANY'));

    const subTabs = document.querySelectorAll('.kb-my-sub-tab');
    const contents = {
        'places': document.getElementById('content-places'),
        'posts': document.getElementById('content-posts'),
        'family': document.getElementById('content-family')
    };

    subTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            subTabs.forEach(t => t.classList.remove('kb-my-active'));
            tab.classList.add('kb-my-active');

            const target = tab.getAttribute('data-target');
            Object.values(contents).forEach(el => { if (el) el.style.display = 'none'; });
            if (contents[target]) contents[target].style.display = 'block';

            if (target === 'posts') loadMyPosts();
            if (target === 'family') loadFamilies();
        });
    });
}

// 주소 검색 (Daum Postcode)
function openAddressSearch(type) {
    new daum.Postcode({
        oncomplete: function (data) {
            const addr = data.address;
            geocoder.addressSearch(addr, function (result, status) {
                if (status === kakao.maps.services.Status.OK) {
                    saveSpecialPlace(type, addr, result[0].y, result[0].x);
                }
            });
        }
    }).open();
}

async function saveSpecialPlace(type, address, lat, lon) {
    try {
        const response = await fetch('/api/map/special-place', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type, address, latitude: lat, longitude: lon })
        });
        if (response.ok) {
            showToast("위치가 등록되었습니다.");
            loadMyPlaces();
        } else showToast("로그인이 필요합니다.", true);
    } catch (e) { console.error(e); }
}

async function loadMyPlaces() {
    try {
        const response = await fetch('/api/map/my-places');
        if (response.ok) {
            myPlaces = await response.json();
            renderMyPlacesAndMarkers();
        }
    } catch (e) { console.error(e); }
}

// 내 장소 렌더링 및 마커 표시
function renderMyPlacesAndMarkers() {
    myPlaceMarkers.forEach(marker => marker.setMap(null));
    myPlaceMarkers = [];

    const home = myPlaces.find(p => p.placeType === 'HOME');
    const company = myPlaces.find(p => p.placeType === 'COMPANY');
    const favorites = myPlaces.filter(p => p.placeType === 'FAVORITE').slice(0, 5);

    const homeText = document.getElementById('home-address-text');
    if (homeText && home) {
        homeText.innerText = home.address;
        createMarker(home.latitude, home.longitude, MARKER_IMGS.HOME, '집');
        document.getElementById('btn-set-home').parentElement.onclick = (e) => {
            if (!e.target.classList.contains('kb-place-action')) moveToPlace(home.latitude, home.longitude);
        };
    }

    const compText = document.getElementById('company-address-text');
    if (compText && company) {
        compText.innerText = company.address;
        createMarker(company.latitude, company.longitude, MARKER_IMGS.COMPANY, '회사');
        document.getElementById('btn-set-company').parentElement.onclick = (e) => {
            if (!e.target.classList.contains('kb-place-action')) moveToPlace(company.latitude, company.longitude);
        };
    }

    const favList = document.getElementById('kb-favorites-list');
    if (favList) {
        favList.innerHTML = '';
        if (favorites.length === 0) {
            favList.innerHTML = '<div style="text-align:center; padding:20px; color:#999; font-size:13px;">없음</div>';
        } else {
            favorites.forEach(fav => {
                const item = document.createElement('div');
                item.className = 'kb-myplace-item';
                item.innerHTML = `
                    <div class="kb-place-icon-box favorite">⭐</div>
                    <div class="kb-place-info"><div class="kb-place-name">${fav.name}</div><div class="kb-place-address">${fav.address}</div></div>
                    <button class="btn-delete-fav" data-id="${fav.id}">🗑️</button>
                `;
                item.addEventListener('click', (e) => {
                    if (!e.target.classList.contains('btn-delete-fav')) moveToPlace(fav.latitude, fav.longitude);
                });
                item.querySelector('.btn-delete-fav').onclick = (e) => {
                    e.stopPropagation();
                    deleteFavorite(fav.id);
                };
                favList.appendChild(item);
                createMarker(fav.latitude, fav.longitude, MARKER_IMGS.FAVORITE, fav.name);
            });
        }
    }
}

// 가족 연락처 로드
async function loadFamilies() {
    const list = document.getElementById('kb-family-list');
    list.innerHTML = '<div style="text-align:center; padding:20px; color:#999;">로딩 중...</div>';

    try {
        const res = await fetch('/api/map/family');
        if (res.ok) {
            const families = await res.json();
            renderFamilies(families);
        } else {
            list.innerHTML = '<div style="text-align:center; padding:20px; color:#999;">로그인이 필요합니다.</div>';
        }
    } catch (e) { console.error(e); }
}

function renderFamilies(families) {
    const list = document.getElementById('kb-family-list');
    list.innerHTML = '';

    if (families.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding:20px; color:#999;">등록된 연락처가 없습니다.</div>';
        return;
    }

    families.forEach(fam => {
        const item = document.createElement('div');
        item.className = 'kb-myplace-item';

        item.innerHTML = `
            <div class="kb-place-icon-box" style="background-color:#fff0f6; color:#e91e63; font-size:16px;">❤️</div>
            <div class="kb-place-info">
                <div class="kb-place-name">${fam.name}</div>
                <div class="kb-place-address">${fam.phone}</div>
            </div>
            <div style="display:flex; gap:5px;">
                <button class="kb-place-action btn-sms" data-phone="${fam.phone}">💬 전송</button>
                <button class="kb-place-action btn-del-fam" data-id="${fam.id}" style="color:#d9534f;">🗑️</button>
            </div>
        `;

        item.querySelector('.btn-sms').onclick = () => {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(pos => {
                    const lat = pos.coords.latitude.toFixed(4);
                    const lon = pos.coords.longitude.toFixed(4);
                    const msg = `[안전네비] 현재 제 위치는 위도:${lat}, 경도:${lon} 입니다. 안전한지 확인해주세요.`;
                    location.href = `sms:${fam.phone}?body=${encodeURIComponent(msg)}`;
                });
            } else alert("위치 정보를 가져올 수 없습니다.");
        };

        item.querySelector('.btn-del-fam').onclick = () => deleteFamily(fam.id);
        list.appendChild(item);
    });
}

// 가족 추가
export async function addFamily() {
    const name = document.getElementById('fam-name').value;
    const phone = document.getElementById('fam-phone').value;

    if (!name || !phone) {
        alert("이름과 전화번호를 입력해주세요.");
        return;
    }

    try {
        const res = await fetch('/api/map/family', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, phone })
        });

        if (res.ok) {
            alert("등록되었습니다.");
            document.getElementById('family-modal').style.display = 'none';
            document.getElementById('fam-name').value = '';
            document.getElementById('fam-phone').value = '';
            loadFamilies();
        } else alert("로그인 필요");
    } catch (e) { console.error(e); }
}

async function deleteFamily(id) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
        await fetch(`/api/map/family/${id}`, { method: 'DELETE' });
        loadFamilies();
    } catch (e) { console.error(e); }
}

// 내 게시글 조회
async function loadMyPosts() {
    const container = document.getElementById('content-posts');
    container.innerHTML = '<div style="text-align:center; padding:40px 0; color:#999;">로딩 중...</div>';

    try {
        const res = await fetch('/api/board/my');
        if (res.ok) {
            const posts = await res.json();
            renderMyPosts(posts);
        } else {
            container.innerHTML = '<div style="text-align:center; padding:30px;">로그인 필요</div>';
        }
    } catch (e) {
        container.innerHTML = '<div style="text-align:center;">오류 발생</div>';
    }
}

function renderMyPosts(posts) {
    const container = document.getElementById('content-posts');
    container.innerHTML = '';

    if (!posts || posts.length === 0) {
        container.innerHTML = '<div style="text-align:center; padding:50px 0; color:#999;">작성된 글이 없습니다.</div>';
        return;
    }

    const listHtml = document.createElement('div');
    listHtml.className = 'kb-myplace-list';

    posts.forEach(post => {
        const item = document.createElement('div');
        item.className = 'kb-myplace-item';

        let icon = '📝';
        if (post.category === '제보') icon = '🚨';
        else if (post.category === '질문') icon = '❓';
        else if (post.category === '잡담') icon = '💬';

        item.innerHTML = `
            <div class="kb-place-icon-box post">${icon}</div>
            <div class="kb-place-info">
                <div class="kb-place-name">${post.title}</div>
                <div class="kb-place-address">${post.content}</div>
                <div class="kb-place-meta">${post.date} · ❤️${post.likeCount} · 💬${post.comments.length}</div>
            </div>
        `;

        item.onclick = () => {
            const pos = new kakao.maps.LatLng(post.latitude, post.longitude);
            map.setLevel(3);
            map.panTo(pos);
            const marker = new kakao.maps.Marker({ position: pos });
            showBoardOverlay(marker, post);
        };
        listHtml.appendChild(item);
    });

    const title = document.createElement('div');
    title.className = 'kb-section-title';
    title.innerText = `작성한 게시글 (${posts.length})`;

    container.appendChild(title);
    container.appendChild(listHtml);
}

// 마커 생성 헬퍼
function createMarker(lat, lon, imageSrc, title) {
    if (!map) return;
    const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(lat, lon),
        image: new kakao.maps.MarkerImage(imageSrc, new kakao.maps.Size(100, 100)),
        title,
        zIndex: 500
    });
    marker.setMap(map);
    myPlaceMarkers.push(marker);
}

function moveToPlace(lat, lon) {
    if (map) {
        map.setLevel(3);
        map.panTo(new kakao.maps.LatLng(lat, lon));
    }
}

async function deleteFavorite(id) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
        await fetch(`/api/map/place/${id}`, { method: 'DELETE' });
        loadMyPlaces();
    } catch (e) { console.error(e); }
}

export async function addToFavorites(name, address, lat, lon) {
    const favCount = myPlaces.filter(p => p.placeType === 'FAVORITE').length;

    if (favCount >= 5) {
        showToast("즐겨찾기는 최대 5개까지 등록 가능합니다.", true);
        return;
    }

    try {
        const res = await fetch('/api/map/favorite', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, address, latitude: lat, longitude: lon })
        });

        if (res.ok) {
            showToast("즐겨찾기에 추가되었습니다!");
            loadMyPlaces();
        } else {
            showToast("정보를 저장할 수 없습니다.", true);
        }
    } catch (e) { console.error(e); }
}