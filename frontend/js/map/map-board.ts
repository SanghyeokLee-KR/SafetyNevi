/**
 * 게시글 관리 (작성, 조회, 실시간 연동)
 */
import { map } from './map-core.js';
import { showToast, toggleLoading, openReportModal } from './map-ui.js';

let isWriteMode = false;
let tempMarker = null;
let currentOverlay = null;
let stompClient = null;
let boardMarkers = [];

const BOARD_MARKERS = {
    '제보': '/img/board/report.png',
    '질문': '/img/board/question.png',
    '잡담': '/img/board/talk.png',
    'default': 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png'
};

// 게시글 로직 초기화
export function setupBoardLogic() {
    const btnWrite = document.getElementById('btn-mode-write');

    // 글쓰기 버튼 핸들러
    btnWrite?.addEventListener('click', () => {
        if(isWriteMode) {
            disableWriteMode();
            return;
        }
        document.getElementById('write-mode-modal').style.display = 'block';
    });

    // 1. 현재 위치(GPS)로 작성
    document.getElementById('btn-mode-gps').onclick = () => {
        document.getElementById('write-mode-modal').style.display = 'none';
        if (!navigator.geolocation) { showToast("GPS 사용 불가", true); return; }

        toggleLoading(true, "위치 확인 중...");
        navigator.geolocation.getCurrentPosition((pos) => {
            toggleLoading(false);
            openWriteModal(pos.coords.latitude, pos.coords.longitude, 'GPS');
        }, () => {
            toggleLoading(false);
            showToast("위치 확인 실패", true);
        });
    };

    // 2. 지도 선택으로 작성
    document.getElementById('btn-mode-map').onclick = () => {
        document.getElementById('write-mode-modal').style.display = 'none';
        isWriteMode = true;
        btnWrite.classList.add('active');
        showToast("지도에서 위치를 선택해주세요.");
        map.setCursor('crosshair');
    };

    // 지도 클릭 이벤트 (작성 모드일 때)
    kakao.maps.event.addListener(map, 'click', function(e) {
        if(!isWriteMode) return;
        openWriteModal(e.latLng.getLat(), e.latLng.getLng(), 'MANUAL');
    });

    // 이미지 뷰어 닫기 처리
    const imgModal = document.getElementById('image-view-modal');
    if(imgModal) {
        imgModal.onclick = (e) => { if(e.target === imgModal || e.target.classList.contains('image-view-close')) imgModal.style.display = "none"; };
    }

    connectWebSocket();
    loadBoards();

    // 전역 함수 등록 (UI 이벤트 핸들링용)
    window.reloadBoardData = () => {
        if(currentOverlay) currentOverlay.setMap(null);
        loadBoards();
    };
    window.deleteBoardPost = deleteBoard;
}

// 웹소켓 연결 (실시간 알림 및 댓글 업데이트)
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // 디버그 로그 비활성화

    stompClient.connect({}, () => {
        // 새 글 알림
        stompClient.subscribe('/topic/board/new', (msg) => {
            const newBoard = JSON.parse(msg.body);
            const blockedUsers = JSON.parse(localStorage.getItem('safety_blocked_users')) || [];

            if(!blockedUsers.includes(newBoard.writer)) {
                addBoardMarker(newBoard);
                showToast(`새 글: ${newBoard.title}`);
            }
        });

        // 삭제 알림
        stompClient.subscribe('/topic/board/delete', () => loadBoards());

        // 댓글 알림
        stompClient.subscribe('/topic/board/comment', (msg) => {
            const data = JSON.parse(msg.body);
            const overlay = document.querySelector('.board-overlay');
            if (overlay && overlay.dataset.boardId == data.boardId) {
                appendRealtimeComment(data.comment, data.parentId, data.boardId);
            }
        });

        // 좋아요 업데이트
        stompClient.subscribe('/topic/board/like', (msg) => {
            const data = JSON.parse(msg.body);
            const el = document.getElementById(`like-count-${data.boardId}`);
            if(el) el.innerText = data.totalLikes;
        });
    });
}

// 작성 모달 열기
function openWriteModal(lat, lng, type) {
    if(tempMarker) tempMarker.setMap(null);
    tempMarker = new kakao.maps.Marker({ position: new kakao.maps.LatLng(lat, lng), map: map });

    const modal = document.getElementById('board-modal');
    modal.style.display = 'block';

    document.getElementById('board-lat').value = lat;
    document.getElementById('board-lon').value = lng;
    document.getElementById('board-location-type').value = type;
    document.getElementById('board-image').value = '';
}

// 작성 모드 종료
function disableWriteMode() {
    isWriteMode = false;
    document.getElementById('btn-mode-write')?.classList.remove('active');
    map.setCursor('default');

    if(tempMarker) { tempMarker.setMap(null); tempMarker = null; }

    document.getElementById('board-modal').style.display = 'none';
    document.getElementById('write-mode-modal').style.display = 'none';
}

// 게시글 등록
export async function saveBoard() {
    const form = {
        title: document.getElementById('board-title').value,
        content: document.getElementById('board-content').value,
        category: document.getElementById('board-category').value,
        lat: document.getElementById('board-lat').value,
        lon: document.getElementById('board-lon').value,
        type: document.getElementById('board-location-type').value,
        image: document.getElementById('board-image').files[0]
    };

    if(!form.title || !form.content) { alert("내용을 입력해주세요."); return; }

    const formData = new FormData();
    formData.append('title', form.title);
    formData.append('content', form.content);
    formData.append('category', form.category);
    formData.append('latitude', form.lat);
    formData.append('longitude', form.lon);
    formData.append('locationType', form.type);
    if(form.image) formData.append('imageFile', form.image);

    try {
        const res = await fetch('/api/board', { method: 'POST', body: formData });
        if(res.ok) {
            showToast("게시글 등록 완료");
            document.getElementById('board-title').value = '';
            document.getElementById('board-content').value = '';
            document.getElementById('board-image').value = '';
            disableWriteMode();
        } else {
            showToast("로그인이 필요합니다", true);
        }
    } catch(e) { console.error(e); }
}

// 게시글 삭제
async function deleteBoard(id) {
    if(!confirm("게시글을 삭제하시겠습니까?")) return;
    try {
        const res = await fetch(`/api/board/${id}`, { method: 'DELETE' });
        if(res.ok) {
            showToast("삭제되었습니다.");
            if(currentOverlay) currentOverlay.setMap(null);
        } else {
            showToast("권한이 없습니다", true);
        }
    } catch(e) { console.error(e); }
}

// 게시글 목록 로드
async function loadBoards() {
    try {
        const res = await fetch('/api/board');
        if(!res.ok) return;
        const boards = await res.json();
        const blockedUsers = JSON.parse(localStorage.getItem('safety_blocked_users')) || [];

        boardMarkers.forEach(m => m.setMap(null));
        boardMarkers = [];

        boards.forEach(board => {
            if(!blockedUsers.includes(board.writer)) {
                addBoardMarker(board);
            }
        });
    } catch(e) {}
}

// 게시글 마커 추가
function addBoardMarker(board) {
    const pos = new kakao.maps.LatLng(board.latitude, board.longitude);
    const imgSrc = BOARD_MARKERS[board.category] || BOARD_MARKERS.default;
    const marker = new kakao.maps.Marker({
        position: pos,
        map: map,
        image: new kakao.maps.MarkerImage(imgSrc, new kakao.maps.Size(60, 60))
    });

    kakao.maps.event.addListener(marker, 'click', () => showBoardOverlay(marker, board));
    boardMarkers.push(marker);
}

// 게시글 상세 오버레이 표시
export function showBoardOverlay(marker, data) {
    if(currentOverlay) currentOverlay.setMap(null);

    const isGps = data.locationType === 'GPS';
    const imageHtml = data.imageUrl ? `<img src="${data.imageUrl}" class="board-image-thumbnail" alt="첨부 이미지">` : '';

    // 버튼 생성 (삭제 또는 신고)
    let actionBtn = '';
    if (data.canDelete) {
        actionBtn = `<span class="board-delete-btn" onclick="window.deleteBoardPost(${data.id})">🗑️</span>`;
    } else {
        actionBtn = `<span class="board-report-btn" id="btn-report-${data.id}" title="신고하기" style="cursor:pointer; margin-left:8px;">🚨</span>`;
    }

    const content = document.createElement('div');
    content.className = 'board-overlay';
    content.dataset.boardId = data.id;
    content.dataset.boardData = JSON.stringify(data);

    content.innerHTML = `
        <div class="board-header">
            <div class="board-writer">
                <span class="board-badge ${data.category}">${data.category}</span> 
                ${data.writer} ${isGps ? '<span class="verified-badge">✅</span>' : ''}
            </div>
            <div style="display:flex; align-items:center;">
                <span class="board-date">${data.date}</span>
                ${actionBtn}
                <span class="board-close" style="margin-left:10px;">✕</span>
            </div>
        </div>
        <div class="board-body">
            ${imageHtml}
            <span class="board-title">${data.title}</span>
            <div class="board-content">${data.content}</div>
        </div>
        <div class="board-actions">
            <div class="action-btn like-btn ${data.liked ? 'liked' : ''}" id="like-btn-${data.id}">
                ${data.liked ? '❤️' : '🤍'} <span id="like-count-${data.id}">${data.likeCount}</span>
            </div>
            <div class="action-btn">💬 <span id="comment-count-${data.id}">${data.comments.length}</span></div>
        </div>
        <div class="board-comments">
            <ul class="comment-list" id="main-comment-list-${data.id}">${renderComments(data.comments, 3)}</ul>
            <div class="comment-form">
                <input type="text" class="comment-input" placeholder="댓글 작성..." id="comment-input-${data.id}">
                <button class="comment-submit" onclick="window.submitComment(${data.id}, null)">게시</button>
            </div>
        </div>
    `;

    // 이벤트 바인딩
    if (data.imageUrl) {
        content.querySelector('.board-image-thumbnail').onclick = () => {
            document.getElementById('full-image').src = data.imageUrl;
            document.getElementById('image-view-modal').style.display = "flex";
        };
    }
    content.querySelector('.board-close').onclick = () => overlay.setMap(null);

    if (!data.canDelete) {
        const reportBtn = content.querySelector(`#btn-report-${data.id}`);
        if(reportBtn) reportBtn.onclick = () => openReportModal('BOARD', data.id, data.title, data.writer);
    }

    content.querySelector('.like-btn').onclick = async () => {
        const res = await fetch(`/api/board/${data.id}/like`, { method: 'POST' });
        if(!res.ok) showToast("로그인 필요", true);
    };

    const overlay = new kakao.maps.CustomOverlay({
        content: content, map: map, position: marker.getPosition(), yAnchor: 1.15, zIndex: 10000
    });
    currentOverlay = overlay;
}

// 댓글 렌더링
function renderComments(comments, limit = 0) {
    if (!comments || comments.length === 0) return '';

    let list = comments;
    let hiddenCount = 0;

    if (limit > 0 && comments.length > limit) {
        list = comments.slice(0, limit);
        hiddenCount = comments.length - limit;
    }

    let html = list.map(c => `
        <li class="comment-item" id="comment-${c.id}">
            <div class="comment-bubble">
                <div class="comment-header">
                    <span class="comment-writer">${c.writer}</span>
                    <span class="comment-time">${c.timeAgo}</span>
                    <span class="btn-reply" onclick="window.toggleReplyForm(${c.id})">답글</span>
                </div>
                <div class="comment-text">${c.content}</div>
            </div>
            <ul class="reply-list" id="reply-list-${c.id}">${renderComments(c.replies)}</ul>
            <div id="reply-form-${c.id}" style="display:none;"></div>
        </li>
    `).join('');

    if (hiddenCount > 0) {
        html += `<button class="btn-more-comments" onclick="window.expandComments(this)">댓글 ${hiddenCount}개 더보기 ▼</button>`;
    }
    return html;
}

// 댓글 더보기
window.expandComments = function(btn) {
    const overlay = btn.closest('.board-overlay');
    const data = JSON.parse(overlay.dataset.boardData);
    const list = overlay.querySelector('.comment-list');
    list.innerHTML = renderComments(data.comments, 0);
};

// 댓글 작성
window.submitComment = async function(boardId, parentId) {
    const inputId = parentId ? `reply-input-${parentId}` : `comment-input-${boardId}`;
    const input = document.getElementById(inputId);
    if(!input.value) return;

    const payload = { content: input.value, parentId: parentId };

    const res = await fetch(`/api/board/${boardId}/comment`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });

    if(res.ok) input.value = '';
    else showToast("로그인이 필요합니다", true);
};

// 실시간 댓글 추가 (WebSocket 수신용)
function appendRealtimeComment(comment, parentId, boardId) {
    const html = `
        <li class="comment-item" id="comment-${comment.id}">
            <div class="comment-bubble">
                <div class="comment-header">
                    <span class="comment-writer">${comment.writer}</span>
                    <span class="comment-time">${comment.timeAgo}</span>
                    <span class="btn-reply" onclick="window.toggleReplyForm(${comment.id})">답글</span>
                </div>
                <div class="comment-text">${comment.content}</div>
            </div>
            <ul class="reply-list" id="reply-list-${comment.id}"></ul>
            <div id="reply-form-${comment.id}" style="display:none;"></div>
        </li>`;

    if(parentId && parentId !== -1) {
        const pList = document.getElementById(`reply-list-${parentId}`);
        if(pList) {
            pList.insertAdjacentHTML('beforeend', html);
            document.getElementById(`reply-form-${parentId}`).style.display = 'none';
        }
    } else {
        const list = document.getElementById(`main-comment-list-${boardId}`);
        if(list) {
            const moreBtn = list.querySelector('.btn-more-comments');
            if(moreBtn) moreBtn.insertAdjacentHTML('beforebegin', html);
            else list.insertAdjacentHTML('beforeend', html);
            list.scrollTop = list.scrollHeight;
        }
    }

    const cnt = document.getElementById(`comment-count-${boardId}`);
    if(cnt) cnt.innerText = parseInt(cnt.innerText) + 1;
}

// 답글 폼 토글
window.toggleReplyForm = function(cid) {
    const box = document.getElementById(`reply-form-${cid}`);
    if(box.style.display === 'block') {
        box.style.display = 'none';
    } else {
        const boardId = box.closest('.board-overlay').dataset.boardId;
        box.innerHTML = `
            <div class="reply-form">
                <input type="text" class="reply-input" placeholder="답글 작성..." id="reply-input-${cid}">
                <button class="reply-submit" onclick="window.submitComment(${boardId}, ${cid})">등록</button>
            </div>`;
        box.style.display = 'block';
        document.getElementById(`reply-input-${cid}`).focus();
    }
};