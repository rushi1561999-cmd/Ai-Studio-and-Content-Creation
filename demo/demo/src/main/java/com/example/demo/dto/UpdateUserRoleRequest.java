package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserRoleRequest {
    @NotBlank
    @Pattern(regexp = "(?i)USER|ADMIN")
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
