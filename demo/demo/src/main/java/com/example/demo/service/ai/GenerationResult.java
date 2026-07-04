package com.example.demo.service.ai;

public class GenerationResult {
    private final boolean success;
    private final String textContent;
    private final String mediaUrl;
    private final String errorMessage;

    private GenerationResult(boolean success, String textContent, String mediaUrl, String errorMessage) {
        this.success = success;
        this.textContent = textContent;
        this.mediaUrl = mediaUrl;
        this.errorMessage = errorMessage;
    }

    public static GenerationResult ok(String textContent) {
        return new GenerationResult(true, textContent, null, null);
    }

    public static GenerationResult okMedia(String mediaUrl) {
        return new GenerationResult(true, null, mediaUrl, null);
    }

    public static GenerationResult okMixed(String textContent, String mediaUrl) {
        return new GenerationResult(true, textContent, mediaUrl, null);
    }

    public static GenerationResult fail(String message) {
        return new GenerationResult(false, null, null, message);
    }

    public boolean isSuccess() { return success; }
    public String getTextContent() { return textContent; }
    public String getMediaUrl() { return mediaUrl; }
    public String getErrorMessage() { return errorMessage; }

    public String primaryResult() {
        if (!success) {
            return "Error: " + errorMessage;
        }
        if (textContent != null && mediaUrl != null) {
            return textContent;
        }
        if (mediaUrl != null) {
            return mediaUrl;
        }
        return textContent != null ? textContent : "";
    }
}
