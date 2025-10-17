package com.example.contacts.service;

import com.example.contacts.dto.WeatherInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WeatherClient {

    @Autowired
    private RestClient.Builder restClientBuilder;
    @Value("${app.weather.base-url}")
    private String baseUrl;
    @Value("${app.weather.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    private RestClient restClient;
    private Duration cacheTtl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public WeatherInfo fetchWeather(String location) {
        if (location == null || location.isBlank()) {
            return new WeatherInfo("Unknown", "No address provided", 0);
        }
        String key = location.trim().toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired(cacheTtl)) {
            log.trace("Returning cached weather for '{}'", location);
            return cached.info();
        }
        log.info("Requesting weather data for '{}' from weather-service", location);
        try {
            WeatherInfo response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/weather")
                            .queryParam("location", location)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(WeatherInfo.class);
            if (response == null) {
                response = new WeatherInfo(location, "Unavailable", 0);
            }
            cache.put(key, new CacheEntry(response, Instant.now()));
            return response;
        } catch (Exception e) {
            log.warn("Weather service unavailable for '{}': {}", location, e.getMessage());
            WeatherInfo fallback = new WeatherInfo(location, "Weather service unavailable", 0);
            cache.put(key, new CacheEntry(fallback, Instant.now()));
            return fallback;
        }
    }

    private record CacheEntry(WeatherInfo info, Instant cachedAt) {
        boolean isExpired(Duration ttl) {
            return cachedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}

