package com.example.demo.dto;

public class AiJobMessage {
    private String jobId;
    private String promptText;
    private String contentType;
    private String modelKey;

    public AiJobMessage() {}

    public AiJobMessage(String jobId, String promptText, String contentType, String modelKey) {
        this.jobId = jobId;
        this.promptText = promptText;
        this.contentType = contentType;
        this.modelKey = modelKey;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
}
