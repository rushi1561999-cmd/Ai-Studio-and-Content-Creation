package com.example.demo.service.prompt;

import com.example.demo.dto.PromptSuggestionRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptQualityServiceTest {

    private final PromptQualityService service = new PromptQualityService();

    @Test
    void scoresSpecificPromptHigherThanVaguePrompt() {
        PromptSuggestionRequest vague = request(
                "Tell me something useful about renewable energy.");
        PromptSuggestionRequest specific = request(
                """
                Write a 700-word professional comparison of solar and wind energy for small-business
                owners. Include a cost table, three practical recommendations, and avoid unsupported claims.
                """);
        specific.setAudience("Small-business owners");
        specific.setTone("Professional and practical");

        assertThat(service.evaluate(specific).score())
                .isGreaterThan(service.evaluate(vague).score());
    }

    @Test
    void fallbackPromptPreservesOriginalIntentAndAddsStructure() {
        PromptSuggestionRequest request = request(
                "Create a launch announcement for a new AI meeting assistant.");

        String optimized = service.buildRuleBasedPrompt(request, service.evaluate(request));

        assertThat(optimized)
                .contains(request.getPromptText())
                .contains("Objective:")
                .contains("Requirements:");
    }

    private PromptSuggestionRequest request(String prompt) {
        PromptSuggestionRequest request = new PromptSuggestionRequest();
        request.setWorkspaceId("workspace-1");
        request.setPromptText(prompt);
        request.setContentType("TEXT");
        return request;
    }
}
