package com.example.contacts.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private Long id;
    private String name;
    private String address;
    private boolean hasPicture;
    private String ownerUsername;
    private Instant updatedAt;
    private WeatherInfo weather;
}
