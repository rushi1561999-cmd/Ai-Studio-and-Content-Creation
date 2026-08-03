package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MarketplacePublishRequest {

    @NotBlank
    @Size(max = 4000)
    private String promptText;
    @Size(max = 80)
    private String category = "Community";

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
