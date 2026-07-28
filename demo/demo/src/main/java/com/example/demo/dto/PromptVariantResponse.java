package com.example.demo.dto;

import java.io.Serializable;

public class PromptVariantResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private String prompt;
    private String reason;

    public PromptVariantResponse() {}

    public PromptVariantResponse(String label, String prompt, String reason) {
        this.label = label;
        this.prompt = prompt;
        this.reason = reason;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
