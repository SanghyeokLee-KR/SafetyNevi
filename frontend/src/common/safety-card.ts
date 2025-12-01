// 홈·재난문자 페이지의 "내 동네 대피 접근성" 카드.
// 내 위치(GPS)로 /api/safety-score를 불러와 점수·가장 가까운 대피소를 보여준다.
// 위치 거부·조회 실패 시 카드를 띄우지 않는다(조용히 숨김). 지도 페이지 패널과는 별개의 독립 모듈.

const GRADE_COLOR: Record<string, string> = {
    "우수": "#28a745", "양호": "#2563eb", "보통": "#ffc107", "주의": "#d9534f",
};

function renderSafetyCard(data) {
    const card = document.getElementById("safety-card");
    const numEl = document.getElementById("sc-num");
    if (!card || !numEl) return;

    const color = GRADE_COLOR[data.grade] || "#64748b";

    // 점수 링 게이지(0~100 비율만큼 채움)
    const ring = document.getElementById("sc-ring");
    if (ring) {
        const R = 19, C = 2 * Math.PI * R;
        const pct = Math.max(0, Math.min(100, data.score)) / 100;
        ring.style.strokeDasharray = String(C);
        ring.style.strokeDashoffset = String(C * (1 - pct));
        ring.style.stroke = color;
    }
    numEl.innerText = String(data.score);
    numEl.style.color = color;

    // 헤드라인: 가장 가까운 운영 대피소 + 도보 추정시간
    const headlineEl = document.getElementById("sc-headline");
    if (headlineEl) {
        if (data.nearestShelter) {
            const s = data.nearestShelter;
            const dist = s.distanceM < 1000 ? `${s.distanceM}m` : `${(s.distanceM / 1000).toFixed(1)}km`;
            headlineEl.innerText = `가까운 대피소 ${dist} · 도보 ${s.walkMinutes}분`;
        } else {
            headlineEl.innerText = "주변 5km 내 운영 대피소가 없어요";
        }
    }

    // 보조 라벨: 등급 + 위험구역 여부
    const subEl = document.getElementById("sc-sub");
    if (subEl) {
        if (data.hazardActive) {
            subEl.innerText = `대피 접근성 ${data.grade} · 현재 ${data.hazardName || "위험구역"} 영향권`;
            subEl.style.color = "#d9534f";
        } else {
            subEl.innerText = `대피 접근성 ${data.grade} · 내 위치 기준`;
            subEl.style.color = "var(--text-muted)";
        }
    }

    card.classList.add("is-ready");
}

function initSafetyCard() {
    const card = document.getElementById("safety-card");
    if (!card || !navigator.geolocation) return;   // 카드 없거나 위치 미지원 → 표시 안 함
    navigator.geolocation.getCurrentPosition(
        async pos => {
            try {
                const res = await fetch(`/api/safety-score?lat=${pos.coords.latitude}&lng=${pos.coords.longitude}`);
                if (!res.ok) return;
                renderSafetyCard(await res.json());
            } catch (e) {
                // 조회 실패 → 카드 숨김 유지
            }
        },
        () => { /* 위치 거부 → 카드 숨김 유지 */ },
        { timeout: 6000, maximumAge: 120000 }
    );
}

if (document.readyState !== "loading") initSafetyCard();
else document.addEventListener("DOMContentLoaded", initSafetyCard);
