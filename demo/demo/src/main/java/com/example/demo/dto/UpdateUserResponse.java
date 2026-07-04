package com.example.demo.dto;

public class UpdateUserResponse {
    private UserProfileResponse user;
    private String token;
    private String message;

    public UserProfileResponse getUser() { return user; }
    public void setUser(UserProfileResponse user) { this.user = user; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
