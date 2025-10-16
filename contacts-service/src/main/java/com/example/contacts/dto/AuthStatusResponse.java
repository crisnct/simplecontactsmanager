package com.example.contacts.dto;

public class AuthStatusResponse {

    private boolean authenticated;
    private String username;

    public AuthStatusResponse() {
    }

    public AuthStatusResponse(boolean authenticated, String username) {
        this.authenticated = authenticated;
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
