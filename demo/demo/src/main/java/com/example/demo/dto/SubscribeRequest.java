package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class SubscribeRequest {

    @NotBlank
    private String planId;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }
}
