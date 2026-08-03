package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequest {
    @NotBlank
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
