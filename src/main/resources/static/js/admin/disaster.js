// 재난 현황 및 시뮬레이션
document.addEventListener('DOMContentLoaded', () => {

    // Kakao Map & Geocoder
    const geocoder = new kakao.maps.services.Geocoder();
    const mapElements = {
        address: document.getElementById('address'),
        areaName: document.getElementById('areaName'),
        areaDisplay: document.getElementById('areaName-display'),
        lat: document.getElementById('lat'),
        lon: document.getElementById('lon')
    };

    document.getElementById('search-address-btn')?.addEventListener('click', () => {
        new daum.Postcode({
            oncomplete: (data) => {
                const sigungu = data.sigungu || data.address.split(' ')[1] || "지역명 미상";

                mapElements.address.value = data.address;
                mapElements.areaName.value = sigungu;
                mapElements.areaDisplay.value = sigungu;

                geocoder.addressSearch(data.address, (result, status) => {
                    if (status === kakao.maps.services.Status.OK) {
                        mapElements.lat.value = result[0].y;
                        mapElements.lon.value = result[0].x;
                    }
                });
            }
        }).open();
    });

    // Load Active Disasters
    const tbody = document.getElementById('disaster-list-body');

    const loadActiveDisasters = async () => {
        if (!tbody) return;

        try {
            const res = await fetch('/api/disaster-zones');
            const list = await res.json();

            tbody.innerHTML = '';

            if (list.length === 0) {
                tbody.innerHTML = `
                    <tr><td colspan="5" style="text-align:center; padding:40px; color:#94a3b8;">
                        현재 발령된 재난이 없습니다. ✅
                    </td></tr>`;
                return;
            }

            list.forEach(item => {
                const isFire = item.disasterType.includes('fire');
                const badgeColor = isFire ? '#ef4444' : '#3b82f6';
                const locationTxt = item.areaName
                    ? `[지역] ${item.areaName}`
                    : `[좌표] ${item.latitude.toFixed(4)}, ${item.longitude.toFixed(4)}`;

                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>#${item.id}</td>
                    <td><span class="status-badge" style="background-color:${badgeColor}">${item.disasterType}</span></td>
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

    // Terminate Disaster
    tbody?.addEventListener('click', async (e) => {
        if (!e.target.classList.contains('btn-terminate')) return;

        const id = e.target.dataset.id;
        if (!confirm("해당 재난 상황을 종료하시겠습니까?")) return;

        try {
            const res = await fetch(`/api/admin/disaster/${id}`, { method: 'DELETE' });
            if (res.ok) loadActiveDisasters();
            else alert("종료 처리에 실패했습니다.");
        } catch (e) {
            alert("서버 통신 오류가 발생했습니다.");
        }
    });

    // Simulator
    const requestSimulate = async (url, payload) => {
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

    // 원형(좌표) 재난
    document.getElementById('simulate-btn-circle')?.addEventListener('click', () => {
        const lat = mapElements.lat.value;
        const lon = mapElements.lon.value;

        if (!lat || !lon) return alert("주소를 검색하여 좌표를 설정해주세요.");

        requestSimulate('/api/admin/simulate', {
            lat, lon,
            type: document.getElementById('type-circle').value,
            radius: document.getElementById('radius').value,
            durationMinutes: document.getElementById('duration-circle').value
        });
    });

    // 지역(행정구역) 재난
    document.getElementById('simulate-btn-area')?.addEventListener('click', () => {
        const area = mapElements.areaName.value;
        if (!area) return alert("지역명이 설정되지 않았습니다.");

        requestSimulate('/api/admin/simulate-area', {
            areaName: area,
            type: document.getElementById('type-area').value,
            durationMinutes: document.getElementById('duration-area').value
        });
    });
});