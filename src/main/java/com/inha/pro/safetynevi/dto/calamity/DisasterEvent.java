package com.inha.pro.safetynevi.dto.calamity;

/**
 * Kafka 재난 이벤트. 중첩 DTO 대신 평면 record 라 JSON 직렬화/역직렬화가 안정적이다.
 * action: "NEW"(발생) | "DELETE"(삭제)
 */
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

    /** 소비 측에서 WebSocket으로 보낼 응답 DTO로 복원 */
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
