// 지도 사이드바의 '실시간 재난문자' 피드. 진입 시 최근분을 REST로 불러오고,
// 새 메시지는 소켓(map-disaster.ts가 /topic/disaster-message 구독)으로 받아 맨 위에 끼운다.
import { escapeHtml } from '../common/escape.js';

const MAX_CARDS = 30;

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
        '<div class="kb-feed-card" data-type="' + cls + '">' +
        '  <div class="kb-feed-head">' +
        '    <span class="kb-feed-badge">' + escapeHtml(msg.disasterType || '기타') + '</span>' +
        '    <span class="kb-feed-area">' + escapeHtml(msg.area || '') + '</span>' +
        '    <span class="kb-feed-time">' + escapeHtml(relativeTime(msg.sentDate || '')) + '</span>' +
        '  </div>' +
        '  <div class="kb-feed-body">' + escapeHtml(msg.content || '') + '</div>' +
        '</div>';
}

// 진입 시 최근 재난문자 로드
export async function loadRecentDisasterMessages() {
    const list = document.getElementById('kb-feed-list');
    if (!list) return;
    try {
        const res = await fetch('/api/disaster-messages/recent');
        if (!res.ok) return;
        const messages = await res.json();
        if (!messages.length) {
            list.innerHTML = '<div class="kb-feed-empty">표시할 재난문자가 없습니다.</div>';
            return;
        }
        list.innerHTML = messages.map(cardHtml).join('');
    } catch (e) {
        console.error('재난문자 피드 로드 실패:', e);
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
