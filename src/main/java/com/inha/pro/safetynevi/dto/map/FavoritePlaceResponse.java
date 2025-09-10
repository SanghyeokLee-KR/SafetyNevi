package com.inha.pro.safetynevi.dto.map;

import com.inha.pro.safetynevi.entity.map.FavoritePlace;
import lombok.Builder;
import lombok.Getter;

/** 내 장소(즐겨찾기) 응답 DTO */
@Getter
@Builder
public class FavoritePlaceResponse {

    private final Long id;
    private final String placeType;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;

    public static FavoritePlaceResponse from(FavoritePlace place) {
        return FavoritePlaceResponse.builder()
                .id(place.getId())
                .placeType(place.getPlaceType())
                .name(place.getName())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .build();
    }
}
