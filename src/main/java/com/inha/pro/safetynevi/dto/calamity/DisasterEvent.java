package com.inha.pro.safetynevi.dto.calamity;

// DTO 통째로 넣으면 역직렬화가 까다로워서 필드 펼친 record 로 둠
public record DisasterEvent(
        String action,   // NEW / DELETE
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
