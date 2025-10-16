package com.example.contacts.dto;

public class UserResponse {
    private String username;

    public UserResponse() {
    }

    public UserResponse(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
