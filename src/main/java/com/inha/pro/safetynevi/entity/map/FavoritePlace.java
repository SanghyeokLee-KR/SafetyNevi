package com.inha.pro.safetynevi.entity.map;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SAFETY_MY_PLACE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FavoritePlace {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLACE_ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "PLACE_TYPE", nullable = false)
    private String placeType; // HOME, COMPANY, FAVORITE

    @Column(name = "PLACE_NAME")
    private String name;

    @Column(name = "ADDRESS", nullable = false)
    private String address;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    public void updateLocation(String address, Double lat, Double lon) {
        this.address = address;
        this.latitude = lat;
        this.longitude = lon;
    }
}