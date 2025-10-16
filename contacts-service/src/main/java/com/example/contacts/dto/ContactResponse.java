package com.example.contacts.dto;

import java.time.Instant;

public class ContactResponse {
    private Long id;
    private String name;
    private String address;
    private String pictureUrl;
    private String ownerUsername;
    private Instant updatedAt;
    private WeatherInfo weather;

    public ContactResponse() {
    }

    public ContactResponse(Long id, String name, String address, String pictureUrl,
                           String ownerUsername, Instant updatedAt, WeatherInfo weather) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.pictureUrl = pictureUrl;
        this.ownerUsername = ownerUsername;
        this.updatedAt = updatedAt;
        this.weather = weather;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public WeatherInfo getWeather() {
        return weather;
    }

    public void setWeather(WeatherInfo weather) {
        this.weather = weather;
    }
}
