package com.example.contacts.dto;

public class WeatherInfo {
    private String location;
    private String description;
    private double temperatureCelsius;

    public WeatherInfo() {
    }

    public WeatherInfo(String location, String description, double temperatureCelsius) {
        this.location = location;
        this.description = description;
        this.temperatureCelsius = temperatureCelsius;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }
}
