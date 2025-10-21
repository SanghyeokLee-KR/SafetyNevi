package com.inha.pro.safetynevi.util.map;

import org.springframework.stereotype.Component;

// WGS84 위경도 -> 기상청 격자좌표(X,Y). 람베르트 정각원추도법(LCC) 공식 그대로 옮긴 거라
// 상수들 못 건드림 (기상청 단기예보 API 가이드 기준)
@Component
public class GpsConverter {

    public LatXLngY convertGpsToGrid(double lat, double lon) {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0;      // 격자 간격(km)
        double SLAT1 = 30.0;    // 투영 위도1
        double SLAT2 = 60.0;    // 투영 위도2
        double OLON = 126.0;    // 기준점 경도
        double OLAT = 38.0;     // 기준점 위도
        double XO = 43;         // 기준점 X좌표
        double YO = 136;        // 기준점 Y좌표
        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        LatXLngY rs = new LatXLngY();
        rs.lat = lat;
        rs.lng = lon;

        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        rs.x = (int) (Math.floor(ra * Math.sin(theta) + XO + 0.5));
        rs.y = (int) (Math.floor(ro - ra * Math.cos(theta) + YO + 0.5));

        return rs;
    }

    public static class LatXLngY {
        public double lat;
        public double lng;
        public int x;
        public int y;
    }
}