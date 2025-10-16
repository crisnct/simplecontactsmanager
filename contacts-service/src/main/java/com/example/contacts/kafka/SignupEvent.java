package com.example.contacts.kafka;

import java.time.Instant;

public class SignupEvent {
    private String username;
    private Instant registeredAt;

    public SignupEvent() {
    }

    public SignupEvent(String username) {
        this.username = username;
        this.registeredAt = Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
