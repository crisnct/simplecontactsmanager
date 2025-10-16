package com.example.weather.service;

import com.example.weather.dto.WeatherResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class WeatherGeneratorService {

    private static final List<String> CONDITIONS = List.of(
            "Sunny",
            "Partly cloudy",
            "Overcast",
            "Light rain",
            "Thunderstorms",
            "Windy",
            "Snow showers"
    );

    public WeatherResponse getWeather(String rawLocation) {
        String location = (rawLocation == null || rawLocation.isBlank())
                ? "Unknown"
                : rawLocation.trim();
        Random random = new Random(seedFrom(location));
        double baseTemperature = 10 + random.nextDouble() * 20;
        double dailyVariance = (LocalDate.now().getDayOfYear() % 10) - 5;
        double temperature = Math.round((baseTemperature + dailyVariance) * 10.0) / 10.0;
        String description = CONDITIONS.get(Math.abs(random.nextInt()) % CONDITIONS.size());
        return new WeatherResponse(location, description, temperature);
    }

    private long seedFrom(String value) {
        return Math.abs(value.toLowerCase().hashCode()) + LocalDate.now().toEpochDay();
    }
}
