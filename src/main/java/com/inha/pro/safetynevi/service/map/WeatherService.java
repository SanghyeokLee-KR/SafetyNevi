package com.inha.pro.safetynevi.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.inha.pro.safetynevi.dto.map.WeatherDto;
import com.inha.pro.safetynevi.util.map.GpsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    // 기상청 날씨는 격자+관측시각 단위라 같은 동네면 캐시 공유해서 호출 줄임 (주소는 더 세밀해서 캐시 안함)
    private final Cache<String, Map<String, String>> weatherCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(2000)
            .build();

    @Value("${api.kma.serviceKey}")
    private String kmaServiceKey;

    @Value("${api.kakao.restKey}")
    private String kakaoRestKey;

    public Mono<WeatherDto> getWeatherInfo(double lat, double lon) {
        // 주소·날씨를 병렬 조회하되, 한쪽이 실패해도 나머지는 보여주도록 각각 폴백을 둔다
        Mono<String> addressMono = getAddressFromKakao(lat, lon)
                .onErrorReturn("주소 정보 없음");

        GpsConverter.LatXLngY grid = gpsConverter.convertGpsToGrid(lat, lon);
        LocalDateTime base = LocalDateTime.now().minusMinutes(45); // 초단기실황은 매시각 자료가 ~40분 뒤 발표돼서 여유를 둠
        String baseDate = base.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = base.format(DateTimeFormatter.ofPattern("HH00"));
        String cacheKey = grid.x + ":" + grid.y + ":" + baseDate + baseTime;

        Map<String, String> cachedWeather = weatherCache.getIfPresent(cacheKey);
        Mono<Map<String, String>> weatherMono = (cachedWeather != null)
                ? Mono.just(cachedWeather)
                : getKmaWeather(grid.x, grid.y, baseDate, baseTime)
                        .map(this::parseKmaWeather)
                        .doOnNext(weather -> { if (weather.containsKey("T1H")) weatherCache.put(cacheKey, weather); }) // 기온 있는 의미있는 값만 캐시(빈/오염값 박제 방지)
                        .onErrorResume(e -> {
                            log.warn("기상청 날씨 조회 실패: {}", e.getMessage());
                            return Mono.just(Map.<String, String>of());
                        });

        return Mono.zip(addressMono, weatherMono)
                .map(tuple -> {
                    String address = tuple.getT1();
                    Map<String, String> weatherMap = tuple.getT2();
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

    private Mono<JsonNode> getKmaWeather(int nx, int ny, String baseDate, String baseTime) {
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