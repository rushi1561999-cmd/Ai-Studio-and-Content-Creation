package com.example.demo.dto;

public class GenerateContentRequest {
    private String promptText;
    private String workspaceId;
    private String contentType = "TEXT";
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
