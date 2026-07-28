package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public class PromptSuggestionRequest {

    @NotBlank
    private String workspaceId;

    @NotBlank
    @Size(min = 20, max = 4_000)
    private String promptText;

    @NotBlank
    @Pattern(regexp = "(?i)TEXT|IMAGE|VIDEO|MIXED")
    private String contentType = "TEXT";

    @Size(max = 200)
    private String goal;

    @Size(max = 120)
    private String audience;

    @Size(max = 80)
    private String tone;

    @Min(1)
    @Max(3)
    private int variantCount = 3;

    public String cacheKey() {
        return normalize(workspaceId) + "|" + normalize(promptText) + "|" + normalize(contentType)
                + "|" + normalize(goal) + "|" + normalize(audience) + "|" + normalize(tone)
                + "|" + variantCount;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public int getVariantCount() { return variantCount; }
    public void setVariantCount(int variantCount) { this.variantCount = variantCount; }
}
