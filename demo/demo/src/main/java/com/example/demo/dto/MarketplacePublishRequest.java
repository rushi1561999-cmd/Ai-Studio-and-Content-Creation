package com.example.demo.dto;

public class MarketplacePublishRequest {

    private String promptText;
    private String category = "Community";

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
