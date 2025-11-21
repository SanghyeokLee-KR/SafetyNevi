// 지도 사이드바의 '실시간 재난문자' 피드. 진입 시 최근분을 REST로 불러오고,
// 새 메시지는 소켓(map-disaster.ts가 /topic/disaster-message 구독)으로 받아 맨 위에 끼운다.
// 카드를 누르면 'feed:focus-area' 이벤트를 쏴 map-disaster.ts가 지도를 그 지역으로 옮긴다.
import { escapeHtml } from '../common/escape.js';

const MAX_CARDS = 30;
let clickAttached = false;
let timerSet = false;

// 재난 종류 → 강조색 클래스(CSS에서 data-type으로 매칭)
const TYPE_CLASS: Record<string, string> = {
    '산불': 'fire', '화재': 'fire', '폭발': 'fire',
    '호우': 'water', '홍수': 'water', '태풍': 'water', '해일': 'water', '침수': 'water',
    '폭염': 'heat',
    '대설': 'snow', '폭설': 'snow', '한파': 'snow',
    '지진': 'quake',
    '가뭄': 'dry',
    '황사': 'dust', '미세먼지': 'dust', '환경오염': 'dust',
};

function typeClass(type: string): string {
    for (const key in TYPE_CLASS) {
        if (type && type.indexOf(key) !== -1) return TYPE_CLASS[key];
    }
    return 'etc';
}

// "2026/06/14 18:43:29" → "3시간 전" 형태의 상대시간
function relativeTime(sentDate: string): string {
    if (!sentDate) return '';
    const ts = Date.parse(sentDate.replace(/\//g, '-').replace(' ', 'T'));
    if (isNaN(ts)) return sentDate;
    const diffMin = Math.floor((Date.now() - ts) / 60000);
    if (diffMin < 1) return '방금';
    if (diffMin < 60) return diffMin + '분 전';
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return diffHr + '시간 전';
    return Math.floor(diffHr / 24) + '일 전';
}

function cardHtml(msg: any): string {
    const cls = typeClass(msg.disasterType || '');
    return '' +
        '<div class="kb-feed-card" data-type="' + cls + '" data-area="' + escapeHtml(msg.area || '') + '" data-time="' + escapeHtml(msg.sentDate || '') + '">' +
        '  <div class="kb-feed-head">' +
        '    <span class="kb-feed-badge">' + escapeHtml(msg.disasterType || '기타') + '</span>' +
        '    <span class="kb-feed-area">' + escapeHtml(msg.area || '') + '</span>' +
        '    <span class="kb-feed-time">' + escapeHtml(relativeTime(msg.sentDate || '')) + '</span>' +
        '  </div>' +
        '  <div class="kb-feed-body">' + escapeHtml(msg.content || '') + '</div>' +
        '</div>';
}

// 카드 클릭 → 그 지역으로 지도 이동(이벤트로 map-disaster.ts에 위임). 모바일은 지도가 보이게 사이드바도 닫는다.
function attachClickHandler(list: HTMLElement) {
    if (clickAttached) return;
    clickAttached = true;
    list.addEventListener('click', (e) => {
        const card = (e.target as HTMLElement).closest('.kb-feed-card') as HTMLElement | null;
        const area = card ? card.dataset.area : null;
        if (!area) return;
        document.dispatchEvent(new CustomEvent('feed:focus-area', { detail: area }));
        if (window.innerWidth <= 768) {
            const closeBtn = document.querySelector('.kb-menu-icon') as HTMLElement | null;
            if (closeBtn) closeBtn.click();
        }
    });
}

// 1분마다 카드의 상대시간을 다시 계산해 갱신("방금"→"1분 전" 등)
function setupTimeRefresh() {
    if (timerSet) return;
    timerSet = true;
    setInterval(function () {
        const list = document.getElementById('kb-feed-list');
        if (!list) return;
        list.querySelectorAll('.kb-feed-card').forEach(function (card) {
            const t = (card as HTMLElement).dataset.time;
            const el = card.querySelector('.kb-feed-time');
            if (t && el) el.textContent = relativeTime(t);
        });
    }, 60000);
}

// 진입 시 최근 재난문자 로드
export async function loadRecentDisasterMessages() {
    const list = document.getElementById('kb-feed-list');
    if (!list) return;
    attachClickHandler(list);
    setupTimeRefresh();
    try {
        const res = await fetch('/api/disaster-messages/recent');
        if (!res.ok) throw new Error('status ' + res.status);
        const messages = await res.json();
        if (!messages.length) {
            list.innerHTML = '<div class="kb-feed-empty">표시할 재난문자가 없습니다.</div>';
            return;
        }
        list.innerHTML = messages.map(cardHtml).join('');
    } catch (e) {
        console.error('재난문자 피드 로드 실패:', e);
        list.innerHTML = '<div class="kb-feed-empty">재난문자를 불러오지 못했어요.</div>';
    }
}

// 소켓으로 새 메시지가 오면 맨 위에 끼우고 하이라이트
export function prependDisasterMessage(msg: any) {
    const list = document.getElementById('kb-feed-list');
    if (!list || !msg) return;

    const empty = list.querySelector('.kb-feed-empty');
    if (empty) list.innerHTML = '';

    const wrap = document.createElement('div');
    wrap.innerHTML = cardHtml(msg);
    const card = wrap.firstElementChild as HTMLElement | null;
    if (!card) return;

    card.classList.add('kb-feed-new');
    list.prepend(card);

    while (list.children.length > MAX_CARDS) {
        list.removeChild(list.lastChild as Node);
    }
}
