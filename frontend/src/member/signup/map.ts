// Daum 우편번호 API 및 Kakao 지도 연동
document.addEventListener('DOMContentLoaded', () => {
    const mapContainer = document.getElementById('mini-map') as HTMLElement;
    const searchBtn = document.getElementById('search-address-btn');

    let map: any = null;
    let marker: any = null;

    if (!searchBtn) return;

    searchBtn.addEventListener('click', () => {
        // Kakao Maps SDK 로드 체크
        if (typeof kakao === 'undefined' || !kakao.maps) {
            alert("지도 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        new daum.Postcode({
            oncomplete: function (data: any) {
                (document.getElementById('address') as HTMLInputElement).value = data.address;
                // 관할 '구'. sigungu가 비면 주소 두 번째 토큰으로 대체
                const sigungu = data.sigungu || data.address.split(' ')[1];
                (document.getElementById('areaName') as HTMLInputElement).value = sigungu;

                // 주소 -> 좌표 변환
                const geocoder = new kakao.maps.services.Geocoder();
                geocoder.addressSearch(data.address, (result: any, status: any) => {
                    if (status !== kakao.maps.services.Status.OK) return;

                    const coords = new kakao.maps.LatLng(result[0].y, result[0].x);

                    (document.getElementById('lat') as HTMLInputElement).value = result[0].y;
                    (document.getElementById('lon') as HTMLInputElement).value = result[0].x;

                    // 미니맵 표시 및 마커 이동
                    mapContainer.style.display = 'block';

                    if (!map) {
                        map = new kakao.maps.Map(mapContainer, { center: coords, level: 3 });
                        marker = new kakao.maps.Marker({ position: coords, map: map });
                    } else {
                        map.relayout();
                        map.setCenter(coords);
                        marker.setPosition(coords);
                    }
                });

                document.getElementById('detailAddress')?.focus();
            }
        }).open();
    });
});
