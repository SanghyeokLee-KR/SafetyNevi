// 재난 현황 및 시뮬레이션
import { escapeHtml } from '../common/escape.js';

document.addEventListener('DOMContentLoaded', () => {

    const geocoder = new kakao.maps.services.Geocoder();
    const mapElements = {
        address: document.getElementById('address') as HTMLInputElement,
        areaName: document.getElementById('areaName') as HTMLInputElement,
        areaDisplay: document.getElementById('areaName-display') as HTMLInputElement,
        lat: document.getElementById('lat') as HTMLInputElement,
        lon: document.getElementById('lon') as HTMLInputElement
    };

    document.getElementById('search-address-btn')?.addEventListener('click', () => {
        new daum.Postcode({
            oncomplete: (data: any) => {
                const sigungu = data.sigungu || data.address.split(' ')[1] || "지역명 미상";

                mapElements.address.value = data.address;
                mapElements.areaName.value = sigungu;
                mapElements.areaDisplay.value = sigungu;

                geocoder.addressSearch(data.address, (result: any, status: any) => {
                    if (status === kakao.maps.services.Status.OK) {
                        mapElements.lat.value = result[0].y;
                        mapElements.lon.value = result[0].x;
                    }
                });
            }
        }).open();
    });

    const tbody = document.getElementById('disaster-list-body');

    const loadActiveDisasters = async (): Promise<void> => {
        if (!tbody) return;

        try {
            const res = await fetch('/api/disaster-zones');
            const list = await res.json();

            tbody.innerHTML = '';

            if (list.length === 0) {
                tbody.innerHTML = `
                    <tr><td colspan="5" style="text-align:center; padding:40px; color:#94a3b8;">
                        현재 발령된 재난이 없습니다.
                    </td></tr>`;
                return;
            }

            list.forEach((item: any) => {
                const isFire = item.disasterType.includes('fire');
                const badgeColor = isFire ? '#ef4444' : '#3b82f6';
                const locationTxt = item.areaName
                    ? `[지역] ${escapeHtml(item.areaName)}`
                    : `[좌표] ${item.latitude.toFixed(4)}, ${item.longitude.toFixed(4)}`;

                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>#${item.id}</td>
                    <td><span class="status-badge" style="background-color:${badgeColor}">${escapeHtml(item.disasterType)}</span></td>
                    <td>${locationTxt}</td>
                    <td>진행중</td>
                    <td><button class="btn-danger-soft btn-terminate" data-id="${item.id}">종료</button></td>
                `;
                tbody.appendChild(row);
            });
        } catch (err) {
            console.error("목록 로드 실패:", err);
        }
    };

    loadActiveDisasters();
    document.getElementById('btn-refresh-disasters')?.addEventListener('click', () => loadActiveDisasters());

    tbody?.addEventListener('click', async (e) => {
        const target = e.target as HTMLElement;
        if (!target.classList.contains('btn-terminate')) return;

        const id = target.dataset.id;
        if (!confirm("해당 재난 상황을 종료하시겠습니까?")) return;

        try {
            const res = await fetch(`/api/admin/disaster/${id}`, { method: 'DELETE' });
            if (res.ok) loadActiveDisasters();
            else alert("종료 처리에 실패했습니다.");
        } catch {
            alert("서버 통신 오류가 발생했습니다.");
        }
    });

    const requestSimulate = async (url: string, payload: Record<string, string>): Promise<void> => {
        try {
            const params = new URLSearchParams(payload).toString();
            const res = await fetch(`${url}?${params}`, { method: 'POST' });

            if (!res.ok) throw new Error();

            alert("재난 경보가 발령되었습니다!");
            loadActiveDisasters();
        } catch {
            alert("발령 실패: 입력 값을 확인하거나 서버 상태를 확인해주세요.");
        }
    };

    // 시나리오 프리셋 → 원형 폼(유형·반경·지속시간) 채우기
    document.querySelectorAll('.btn-preset').forEach((btn) => {
        btn.addEventListener('click', () => {
            const b = btn as HTMLElement;
            (document.getElementById('type-circle') as HTMLSelectElement).value = b.dataset.type || 'fire';
            (document.getElementById('radius') as HTMLInputElement).value = b.dataset.radius || '1000';
            (document.getElementById('duration-circle') as HTMLInputElement).value = b.dataset.duration || '30';
        });
    });

    // 영향 미리보기: 이 반경에 닿는 대피소 수 + 알림 대상 구독자 수
    document.getElementById('btn-impact-circle')?.addEventListener('click', async () => {
        const lat = mapElements.lat.value, lon = mapElements.lon.value;
        const radius = (document.getElementById('radius') as HTMLInputElement).value;
        const box = document.getElementById('impact-preview');
        if (!lat || !lon) { alert('먼저 주소를 검색해 중심 위치를 설정하세요.'); return; }
        if (box) { box.style.display = 'block'; box.textContent = '영향 계산 중…'; }
        try {
            const res = await fetch(`/api/admin/simulate/impact?lat=${lat}&lon=${lon}&radius=${radius}`);
            if (!res.ok) throw new Error();
            const d = await res.json();
            if (box) box.innerHTML = `예상 영향 — 반경 ${Number(radius).toLocaleString()}m 안 대피소 <b>${d.shelterCount}곳</b> · 알림 대상 구독자 <b>${d.subscriberCount}명</b>`;
        } catch {
            if (box) box.textContent = '영향 계산에 실패했습니다. 잠시 후 다시 시도하세요.';
        }
    });

    // QR 온보딩: 지역 선택 → 그 지역 온보딩 URL의 QR 이미지 + 링크 갱신
    const qrRegion = document.getElementById('qr-region') as HTMLSelectElement | null;
    if (qrRegion) {
        const updateQr = () => {
            const onboardUrl = `${location.origin}/onboard?region=${encodeURIComponent(qrRegion.value)}`;
            const img = document.getElementById('qr-img') as HTMLImageElement | null;
            const link = document.getElementById('qr-url') as HTMLAnchorElement | null;
            if (img) img.src = `/api/admin/qr?text=${encodeURIComponent(onboardUrl)}&size=200`;
            if (link) { link.href = onboardUrl; link.textContent = onboardUrl; }
        };
        qrRegion.addEventListener('change', updateQr);
        updateQr();
    }

    // 원형(좌표) 재난
    document.getElementById('simulate-btn-circle')?.addEventListener('click', () => {
        const lat = mapElements.lat.value;
        const lon = mapElements.lon.value;

        if (!lat || !lon) return alert("주소를 검색하여 좌표를 설정해주세요.");

        requestSimulate('/api/admin/simulate', {
            lat, lon,
            type: (document.getElementById('type-circle') as HTMLInputElement).value,
            radius: (document.getElementById('radius') as HTMLInputElement).value,
            durationMinutes: (document.getElementById('duration-circle') as HTMLInputElement).value
        });
    });

    // 지역(행정구역) 재난
    document.getElementById('simulate-btn-area')?.addEventListener('click', () => {
        const area = mapElements.areaName.value;
        if (!area) return alert("지역명이 설정되지 않았습니다.");

        requestSimulate('/api/admin/simulate-area', {
            areaName: area,
            type: (document.getElementById('type-area') as HTMLInputElement).value,
            durationMinutes: (document.getElementById('duration-area') as HTMLInputElement).value
        });
    });
});
