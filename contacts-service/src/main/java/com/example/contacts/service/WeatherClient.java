package com.example.contacts.service;

import com.example.contacts.dto.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    private final RestClient restClient;
    private final Duration cacheTtl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public WeatherClient(RestClient.Builder builder,
                         @Value("${app.weather.base-url}") String baseUrl,
                         @Value("${app.weather.cache-ttl-seconds:300}") long cacheTtlSeconds) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public WeatherInfo fetchWeather(String location) {
        if (location == null || location.isBlank()) {
            return new WeatherInfo("Unknown", "No address provided", 0);
        }
        String key = location.trim().toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired(cacheTtl)) {
            return cached.info();
        }
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
