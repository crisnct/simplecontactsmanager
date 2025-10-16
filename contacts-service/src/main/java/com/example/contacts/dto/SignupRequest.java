package com.example.contacts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank
    @Size(min = 3, max = 150)
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
             message = "Password must contain at least one letter and one digit")
    private String password;

    public SignupRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
