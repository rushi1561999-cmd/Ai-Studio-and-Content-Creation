package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class PromptSuggestionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originalPrompt;
    private String optimizedPrompt;
    private int qualityScore;
    private List<String> suggestions = new ArrayList<>();
    private List<PromptVariantResponse> variants = new ArrayList<>();
    private String source;

    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }

    public String getOptimizedPrompt() { return optimizedPrompt; }
    public void setOptimizedPrompt(String optimizedPrompt) { this.optimizedPrompt = optimizedPrompt; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public List<PromptVariantResponse> getVariants() { return variants; }
    public void setVariants(List<PromptVariantResponse> variants) {
        this.variants = variants == null ? new ArrayList<>() : variants;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
