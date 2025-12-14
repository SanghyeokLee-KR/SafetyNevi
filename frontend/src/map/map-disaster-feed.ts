// 지도 사이드바의 '실시간 재난문자' 피드. 진입 시 최근분을 REST로 불러오고,
// 새 메시지는 소켓(map-disaster.ts가 /topic/disaster-message 구독)으로 받아 맨 위에 끼운다.
// 카드를 누르면 'feed:focus-area' 이벤트를 쏴 map-disaster.ts가 지도를 그 지역으로 옮긴다.
import { escapeHtml } from '../common/escape.js';
import { fetchRetry } from '../common/fetch-retry.js';

let clickAttached = false;
let timerSet = false;

// 무한 스크롤 상태: 가장 오래된(맨 아래) 카드의 커서 + 진행 플래그
const PAGE_SIZE = 20;
let oldestDate: string | null = null;
let oldestId: number | null = null;
let loadingMore = false;
let reachedEnd = false;
let feedObserver: IntersectionObserver | null = null;

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

// 지역명에서 시/도 추출, 서버 WebPushService.provinceOf와 같은 규칙(피드 지역 필터용)
const PROVINCE_RULES: [string, string][] = [
    ['서울', '서울'], ['부산', '부산'], ['대구', '대구'], ['인천', '인천'], ['광주', '광주'],
    ['대전', '대전'], ['울산', '울산'], ['세종', '세종'], ['경기', '경기'], ['강원', '강원'],
    ['충청북', '충북'], ['충북', '충북'], ['충청남', '충남'], ['충남', '충남'],
    ['전라북', '전북'], ['전북', '전북'], ['전라남', '전남'], ['전남', '전남'],
    ['경상북', '경북'], ['경북', '경북'], ['경상남', '경남'], ['경남', '경남'], ['제주', '제주'],
];

function provinceOf(area: string): string | null {
    if (!area) return null;
    for (const [needle, prov] of PROVINCE_RULES) {
        if (area.indexOf(needle) !== -1) return prov;
    }
    return null;
}

let feedRegion = '전국';
let regionAttached = false;

// 선택한 시/도에 해당하는 카드만 보이게. 걸러진 게 없으면 안내 한 줄.
function applyRegionFilter() {
    const list = document.getElementById('kb-feed-list');
    if (!list) return;
    let visible = 0;
    list.querySelectorAll('.kb-feed-card').forEach((c) => {
        const card = c as HTMLElement;
        const ok = feedRegion === '전국' || provinceOf(card.dataset.area || '') === feedRegion;
        card.style.display = ok ? '' : 'none';
        if (ok) visible++;
    });
    let note = list.querySelector('.kb-feed-note') as HTMLElement | null;
    if (visible === 0 && feedRegion !== '전국') {
        if (!note) {
            note = document.createElement('div');
            note.className = 'kb-feed-note';
            list.appendChild(note);
        }
        note.textContent = feedRegion + ' 지역의 최근 재난문자가 없어요.';
        note.style.display = '';
    } else if (note) {
        note.style.display = 'none';
    }
}

// 지역 select 변경 → 필터 다시 적용 (한 번만 바인딩)
function setupRegionFilter() {
    if (regionAttached) return;
    const sel = document.getElementById('kb-feed-region') as HTMLSelectElement | null;
    if (!sel) return;
    regionAttached = true;
    sel.addEventListener('change', () => {
        feedRegion = sel.value;
        applyRegionFilter();
    });
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
        '<div class="kb-feed-card" role="button" tabindex="0" data-id="' + (msg.id != null ? msg.id : '') + '" data-type="' + cls + '" data-area="' + escapeHtml(msg.area || '') + '" data-time="' + escapeHtml(msg.sentDate || '') + '">' +
        '  <div class="kb-feed-head">' +
        '    <span class="kb-feed-badge">' + escapeHtml(msg.disasterType || '기타') + '</span>' +
        '    <span class="kb-feed-area">' + escapeHtml(msg.area || '') + '</span>' +
        '    <span class="kb-feed-time">' + escapeHtml(relativeTime(msg.sentDate || '')) + '</span>' +
        '  </div>' +
        '  <div class="kb-feed-body">' + escapeHtml(msg.content || '') + '</div>' +
        '</div>';
}

// 카드 활성화(클릭/엔터·스페이스 공통) → 그 지역으로 지도 이동(이벤트로 map-disaster.ts에 위임).
// 모바일은 지도가 보이게 사이드바도 닫는다.
function activateCard(card: HTMLElement) {
    const area = card.dataset.area;
    if (!area) return;
    document.dispatchEvent(new CustomEvent('feed:focus-area', { detail: area }));
    if (window.innerWidth <= 768) {
        const closeBtn = document.querySelector('.kb-menu-icon') as HTMLElement | null;
        if (closeBtn) closeBtn.click();
    }
}

// 카드는 마우스뿐 아니라 키보드(Tab→Enter/Space)로도 눌러야 한다(role=button, tabindex=0).
function attachClickHandler(list: HTMLElement) {
    if (clickAttached) return;
    clickAttached = true;
    list.addEventListener('click', (e) => {
        const card = (e.target as HTMLElement).closest('.kb-feed-card') as HTMLElement | null;
        if (card) activateCard(card);
    });
    list.addEventListener('keydown', (e) => {
        if (e.key !== 'Enter' && e.key !== ' ') return;
        const card = (e.target as HTMLElement).closest('.kb-feed-card') as HTMLElement | null;
        if (card) { e.preventDefault(); activateCard(card); }
    });
}

// 새 재난문자가 오면 화면에 안 보이는 라이브 영역에 한 줄 넣어 스크린리더가 읽게 한다(긴급정보).
function announceNew(msg: any) {
    const region = document.getElementById('kb-feed-announce');
    if (!region) return;
    const type = msg.disasterType || '재난';
    const area = msg.area || '';
    region.textContent = '새 재난문자. ' + (area ? area + ' ' : '') + type + '.';
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

// 피드가 스크롤되는 실제 컨테이너(사이드바)를 런타임에 찾는다, IntersectionObserver root로 씀.
function findScrollParent(el: HTMLElement | null): HTMLElement {
    let node = el ? el.parentElement : null;
    while (node && node !== document.body) {
        const oy = getComputedStyle(node).overflowY;
        if (oy === 'auto' || oy === 'scroll' || oy === 'overlay') return node;
        node = node.parentElement;
    }
    return (document.scrollingElement as HTMLElement) || document.documentElement;
}

// 센티넬(맨 아래) 상태 표시: 로딩 스피너 / 끝 안내 / 숨김
function setSentinel(state: '' | 'loading' | 'end') {
    const s = document.getElementById('kb-feed-sentinel');
    if (!s) return;
    s.className = 'kb-feed-sentinel' + (state ? ' is-' + state : '');
    s.textContent = state === 'loading' ? '불러오는 중…' : state === 'end' ? '마지막 재난문자입니다' : '';
}

// 센티넬이 보이면 과거 문자를 이어 로드. 화면이 짧으면 re-observe로 자동으로 한 번 더 채운다.
function setupInfiniteScroll(list: HTMLElement) {
    const sentinel = document.getElementById('kb-feed-sentinel');
    if (!sentinel) return;
    if (feedObserver) feedObserver.disconnect();
    feedObserver = new IntersectionObserver((entries) => {
        if (entries[0] && entries[0].isIntersecting) loadOlder();
    }, { root: findScrollParent(list), rootMargin: '160px 0px' });
    feedObserver.observe(sentinel);
}

// 커서(가장 오래된 카드)보다 과거 문자 PAGE_SIZE건을 이어붙임
async function loadOlder() {
    if (loadingMore || reachedEnd || oldestId == null || oldestDate == null) return;
    const list = document.getElementById('kb-feed-list');
    if (!list) return;
    loadingMore = true;
    setSentinel('loading');
    try {
        const url = '/api/disaster-messages/older?beforeDate=' + encodeURIComponent(oldestDate)
            + '&beforeId=' + oldestId + '&size=' + PAGE_SIZE;
        const older = await (await fetchRetry(url)).json();
        if (older.length) {
            list.insertAdjacentHTML('beforeend', older.map(cardHtml).join(''));
            const last = older[older.length - 1];
            oldestDate = last.sentDate;
            oldestId = last.id;
            applyRegionFilter();
        }
        if (older.length < PAGE_SIZE) {
            reachedEnd = true;
            const sentinel = document.getElementById('kb-feed-sentinel');
            if (feedObserver && sentinel) feedObserver.unobserve(sentinel);
            setSentinel(list.querySelector('.kb-feed-card') ? 'end' : '');
        }
    } catch (e) {
        console.error('과거 재난문자 로드 실패:', e);
    } finally {
        loadingMore = false;
        if (!reachedEnd) {
            setSentinel('');
            // 화면이 아직 안 찼으면 센티넬이 계속 보일 테니 re-observe로 한 번 더 트리거
            const sentinel = document.getElementById('kb-feed-sentinel');
            if (feedObserver && sentinel) { feedObserver.unobserve(sentinel); feedObserver.observe(sentinel); }
        }
    }
}

// 진입 시 최근 재난문자 로드
export async function loadRecentDisasterMessages() {
    const list = document.getElementById('kb-feed-list');
    if (!list) return;
    // 상태 초기화(재호출 대비)
    oldestDate = null; oldestId = null; loadingMore = false; reachedEnd = false;
    if (feedObserver) { feedObserver.disconnect(); feedObserver = null; }
    setSentinel('');
    attachClickHandler(list);
    setupTimeRefresh();
    setupRegionFilter();
    try {
        const res = await fetchRetry('/api/disaster-messages/recent');
        const messages = await res.json();
        if (!messages.length) {
            list.innerHTML = '<div class="kb-feed-empty">표시할 재난문자가 없습니다.</div>';
            return;
        }
        list.innerHTML = messages.map(cardHtml).join('');
        const last = messages[messages.length - 1];
        oldestDate = last.sentDate;
        oldestId = last.id;
        reachedEnd = messages.length < PAGE_SIZE;
        applyRegionFilter();
        if (reachedEnd) {
            setSentinel('end');
        } else {
            setupInfiniteScroll(list);
        }
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

    // (무한 스크롤로 아래에 과거분이 쌓이므로 옛 캡 제거, 새 글은 위에만 끼운다)
    applyRegionFilter();   // 현재 지역 필터를 새 카드에도 반영
    if (card.style.display !== 'none') announceNew(msg);   // 보이는(관심지역) 새 글만 음성 안내
}
