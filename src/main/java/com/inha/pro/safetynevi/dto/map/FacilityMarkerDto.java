package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.Facility;
import com.inha.pro.safetynevi.entity.Hospital;
import com.inha.pro.safetynevi.entity.Shelter;
import lombok.Getter;

/**
 * 지도 마커만 찍는 데 필요한 최소 필드.
 *
 * <p>영역 조회는 한 번에 1,500건까지 내려간다. 부하 측정에서 이 경로 하나가 387KB 응답과
 * 초당 52MB 전송을 만들어 병목으로 잡혔다. 그런데 화면에서 마커를 찍는
 * {@code map-marker.js} 는 주소를 쓰지 않는다. 주소는 마커를 눌렀을 때
 * {@code /api/facilities/detail/{id}} 가 따로 내려준다.
 *
 * <p>한글 주소는 JSON 에서 한 글자가 3바이트라 목록에서 가장 무거운 필드다.
 * 목록에서 빼고 상세에만 두면 화면 동작은 그대로면서 전송량이 준다.
 * 검색 결과는 주소를 보여줘야 하므로 {@link FacilityDto} 를 그대로 쓴다.
 */
@Getter
public class FacilityMarkerDto {

    private final Long id;
    private final String type;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String operatingStatus;
    private final Integer maxCapacity;

    public FacilityMarkerDto(Facility facility) {
        this.id = facility.getId();
        this.type = facility.getType();
        this.name = facility.getName();
        this.latitude = facility.getLatitude();
        this.longitude = facility.getLongitude();

        if (facility instanceof Hospital hospital) {
            this.operatingStatus = hospital.getOperatingStatus();
            this.maxCapacity = 0;
        } else if (facility instanceof Shelter shelter) {
            this.operatingStatus = shelter.getOperatingStatus();
            this.maxCapacity = shelter.getMaxCapacity();
        } else {
            this.operatingStatus = "N/A";
            this.maxCapacity = 0;
        }
    }
}
