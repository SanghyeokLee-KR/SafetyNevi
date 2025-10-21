package com.inha.pro.safetynevi.controller.map;

import com.inha.pro.safetynevi.dto.map.WeatherDto;
import com.inha.pro.safetynevi.service.map.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/api/weather")
    public Mono<WeatherDto> getWeather(@RequestParam double lat, @RequestParam double lon) {
        return weatherService.getWeatherInfo(lat, lon);
    }
}