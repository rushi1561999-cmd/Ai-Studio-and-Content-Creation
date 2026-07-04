package com.example.demo.dto;

public class PasswordResetResponse {
    private String message;
    private String resetUrl;

    public PasswordResetResponse() {}

    public PasswordResetResponse(String message, String resetUrl) {
        this.message = message;
        this.resetUrl = resetUrl;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResetUrl() { return resetUrl; }
    public void setResetUrl(String resetUrl) { this.resetUrl = resetUrl; }
}
