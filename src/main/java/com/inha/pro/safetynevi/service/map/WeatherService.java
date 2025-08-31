package com.inha.pro.safetynevi.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.inha.pro.safetynevi.dto.map.WeatherDto;
import com.inha.pro.safetynevi.util.map.GpsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 날씨 정보 서비스
 * - WebClient(Non-blocking)를 사용하여 기상청 API와 카카오 주소 API를 병렬 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final GpsConverter gpsConverter;
    private final WebClient webClient = WebClient.create();

    @Value("${api.kma.serviceKey}")
    private String kmaServiceKey;

    @Value("${api.kakao.restKey}")
    private String kakaoRestKey;

    public Mono<WeatherDto> getWeatherInfo(double lat, double lon) {
        // 주소 조회와 날씨 조회를 병렬(zip)로 처리
        Mono<String> addressMono = getAddressFromKakao(lat, lon);
        GpsConverter.LatXLngY grid = gpsConverter.convertGpsToGrid(lat, lon);
        Mono<JsonNode> weatherMono = getKmaWeather(grid.x, grid.y);

        return Mono.zip(addressMono, weatherMono)
                .map(tuple -> {
                    String address = tuple.getT1();
                    JsonNode weatherData = tuple.getT2();
                    Map<String, String> weatherMap = parseKmaWeather(weatherData);

                    String status = combineWeatherStatus(
                            weatherMap.getOrDefault("PTY", "0"),
                            weatherMap.getOrDefault("SKY", "0")
                    );

                    return WeatherDto.builder()
                            .address(address)
                            .temp(weatherMap.getOrDefault("T1H", "N/A"))
                            .weatherStatus(status)
                            .build();
                });
    }

    private Mono<String> getAddressFromKakao(double lat, double lon) {
        String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" + lon + "&y=" + lat;
        return webClient.get()
                .uri(url)
                .header("Authorization", "KakaoAK " + kakaoRestKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    try {
                        JsonNode doc = jsonNode.get("documents").get(0).get("address");
                        return doc.get("region_2depth_name").asText() + " " + doc.get("region_3depth_name").asText();
                    } catch (Exception e) {
                        return "주소 정보 없음";
                    }
                });
    }

    private Mono<JsonNode> getKmaWeather(int nx, int ny) {
        LocalDateTime now = LocalDateTime.now().minusMinutes(30);
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HH00"));

        String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" +
                "?serviceKey=" + kmaServiceKey +
                "&pageNo=1&numOfRows=10&dataType=JSON" +
                "&base_date=" + baseDate + "&base_time=" + baseTime + "&nx=" + nx + "&ny=" + ny;

        return webClient.get().uri(url).retrieve().bodyToMono(JsonNode.class);
    }

    private Map<String, String> parseKmaWeather(JsonNode weatherData) {
        Map<String, String> map = new HashMap<>();
        try {
            JsonNode items = weatherData.get("response").get("body").get("items").get("item");
            for (JsonNode item : items) {
                String category = item.get("category").asText();
                if (List.of("T1H", "SKY", "PTY").contains(category)) {
                    map.put(category, item.get("obsrValue").asText());
                }
            }
        } catch (Exception e) { log.error("Weather parse error", e); }
        return map;
    }

    private String combineWeatherStatus(String pty, String sky) {
        if (!"0".equals(pty)) {
            return switch (pty) {
                case "1" -> "비";
                case "2" -> "비/눈";
                case "3" -> "눈";
                case "5" -> "빗방울";
                case "6" -> "빗방울/눈날림";
                case "7" -> "눈날림";
                default -> "맑음";
            };
        }
        return switch (sky) {
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "맑음";
        };
    }
}