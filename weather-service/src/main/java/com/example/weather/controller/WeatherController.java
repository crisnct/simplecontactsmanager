package com.example.weather.controller;

import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherGeneratorService weatherGeneratorService;

    public WeatherController(WeatherGeneratorService weatherGeneratorService) {
        this.weatherGeneratorService = weatherGeneratorService;
    }

    @GetMapping
    public ResponseEntity<WeatherResponse> getWeather(@RequestParam(name = "location", required = false) String location) {
        return ResponseEntity.ok(weatherGeneratorService.getWeather(location));
    }
}
