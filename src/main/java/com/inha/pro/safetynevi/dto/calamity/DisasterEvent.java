package com.inha.pro.safetynevi.dto.calamity;

// 카프카로 주고받는 재난 이벤트. DTO 통째로 넣으면 역직렬화가 까다로워서 필드 펼친 record 로 둔다.
// action: NEW / DELETE
public record DisasterEvent(
        String action,
        Long id,
        String disasterType,
        Double latitude,
        Double longitude,
        Double radius,
        String areaName
) {
    public static DisasterEvent ofNew(DisasterZoneResponse z) {
        return new DisasterEvent("NEW", z.getId(), z.getDisasterType(),
                z.getLatitude(), z.getLongitude(), z.getRadius(), z.getAreaName());
    }

    public static DisasterEvent ofDelete(Long id) {
        return new DisasterEvent("DELETE", id, null, null, null, null, null);
    }

    // 받는 쪽에서 다시 응답 DTO 로
    public DisasterZoneResponse toZone() {
        return DisasterZoneResponse.builder()
                .id(id)
                .disasterType(disasterType)
                .latitude(latitude)
                .longitude(longitude)
                .radius(radius)
                .areaName(areaName)
                .build();
    }
}
