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
                // 1. 주소 및 관할구역 입력
                (document.getElementById('address') as HTMLInputElement).value = data.address;
                // '구' 단위 추출 (없으면 공백 기준 두 번째 단어)
                const sigungu = data.sigungu || data.address.split(' ')[1];
                (document.getElementById('areaName') as HTMLInputElement).value = sigungu;

                // 2. 주소 -> 좌표 변환 (Geocoding)
                const geocoder = new kakao.maps.services.Geocoder();
                geocoder.addressSearch(data.address, (result: any, status: any) => {
                    if (status !== kakao.maps.services.Status.OK) return;

                    const coords = new kakao.maps.LatLng(result[0].y, result[0].x);

                    // Hidden input에 좌표 저장
                    (document.getElementById('lat') as HTMLInputElement).value = result[0].y;
                    (document.getElementById('lon') as HTMLInputElement).value = result[0].x;

                    // 3. 미니맵 표시 및 마커 이동
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

                // 상세 주소 입력창으로 포커스 이동
                document.getElementById('detailAddress')?.focus();
            }
        }).open();
    });
});
