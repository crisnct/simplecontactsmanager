package com.example.contacts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String address;

    @Size(max = 500)
    private String pictureUrl;

    public ContactRequest() {
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
}
