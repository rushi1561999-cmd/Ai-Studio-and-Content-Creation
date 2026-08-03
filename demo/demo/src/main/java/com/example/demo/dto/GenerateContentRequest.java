package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GenerateContentRequest {
    @NotBlank
    @Size(max = 4000)
    private String promptText;
    @NotBlank
    private String workspaceId;
    @Pattern(regexp = "(?i)TEXT|IMAGE|VIDEO|MIXED")
    private String contentType = "TEXT";
    @Size(max = 100)
    private String modelKey;

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
}
