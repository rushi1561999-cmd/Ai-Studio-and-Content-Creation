package com.example.demo.service.prompt;

import com.example.demo.dto.PromptSuggestionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PromptQualityService {

    public PromptQuality evaluate(PromptSuggestionRequest request) {
        String prompt = request.getPromptText().trim();
        String lower = prompt.toLowerCase(Locale.ROOT);
        int score = 20;
        List<String> suggestions = new ArrayList<>();

        if (prompt.length() >= 80) {
            score += 15;
        } else {
            suggestions.add("Add background and the outcome you want the model to produce.");
        }

        if (containsAny(lower, "create", "write", "generate", "design", "explain", "summarize", "compare")) {
            score += 15;
        } else {
            suggestions.add("Start with a clear action such as create, explain, compare, or design.");
        }

        if (hasText(request.getAudience()) || containsAny(lower, "audience", "reader", "customer", "beginner", "expert")) {
            score += 15;
        } else {
            suggestions.add("Name the target audience and its level of knowledge.");
        }

        if (hasText(request.getTone()) || containsAny(lower, "tone", "professional", "friendly", "formal", "cinematic")) {
            score += 10;
        } else {
            suggestions.add("Specify the desired tone or visual style.");
        }

        if (containsAny(lower, "format", "table", "bullet", "json", "steps", "resolution", "aspect ratio")) {
            score += 15;
        } else {
            suggestions.add("Describe the expected output format, structure, or dimensions.");
        }

        if (containsAny(lower, "must", "avoid", "limit", "between", "maximum", "minimum", "include")) {
            score += 10;
        } else {
            suggestions.add("Add constraints: required details, length, and anything to avoid.");
        }

        return new PromptQuality(Math.min(score, 100), suggestions);
    }

    public String buildRuleBasedPrompt(PromptSuggestionRequest request, PromptQuality quality) {
        StringBuilder optimized = new StringBuilder();
        optimized.append("Act as an expert ")
                .append(roleFor(request.getContentType()))
                .append(".\n\nObjective:\n")
                .append(hasText(request.getGoal()) ? request.getGoal().trim() : request.getPromptText().trim())
                .append("\n\nOriginal context:\n")
                .append(request.getPromptText().trim());

        if (hasText(request.getAudience())) {
            optimized.append("\n\nTarget audience:\n").append(request.getAudience().trim());
        }
        if (hasText(request.getTone())) {
            optimized.append("\n\nTone and style:\n").append(request.getTone().trim());
        }

        optimized.append("\n\nRequirements:\n")
                .append("- Be specific, accurate, and internally consistent.\n")
                .append("- Include useful detail without unnecessary repetition.\n")
                .append("- State any assumptions before the final output.\n")
                .append("- Return a polished result ready to use.");

        return optimized.toString();
    }

    private String roleFor(String contentType) {
        if (contentType == null) {
            return "content strategist";
        }
        return switch (contentType.toUpperCase(Locale.ROOT)) {
            case "IMAGE" -> "visual art director and prompt engineer";
            case "VIDEO" -> "video director and storyboard designer";
            case "MIXED" -> "creative director and content strategist";
            default -> "writer and content strategist";
        };
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PromptQuality(int score, List<String> suggestions) {}
}
