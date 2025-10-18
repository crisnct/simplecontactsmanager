package com.example.weather.controller;

import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
class WeatherControllerIntegrationTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherGeneratorService weatherGeneratorService;

    @Test
    void returnsWeatherForRequestedLocation() throws Exception {
        WeatherResponse response = new WeatherResponse("Berlin", "Cloudy", 16.5);
        when(weatherGeneratorService.getWeather("Berlin")).thenReturn(response);

        mockMvc.perform(get("/api/weather").param("location", "Berlin"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.location").value("Berlin"))
                .andExpect(jsonPath("$.description").value("Cloudy"))
                .andExpect(jsonPath("$.temperatureCelsius").value(16.5));

        verify(weatherGeneratorService).getWeather(eq("Berlin"));
    }

    @Test
    void fallsBackWhenLocationMissing() throws Exception {
        WeatherResponse response = new WeatherResponse("Unknown", "Sunny", 21.0);
        when(weatherGeneratorService.getWeather(null)).thenReturn(response);

        mockMvc.perform(get("/api/weather"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.location").value("Unknown"));

        verify(weatherGeneratorService).getWeather(null);
    }
}
