package com.example.demo.controller;

import com.example.demo.dto.PromptSuggestionRequest;
import com.example.demo.dto.PromptSuggestionResponse;
import com.example.demo.entity.User;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.prompt.PromptAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt-assistant")
public class PromptAssistantController {

    private final PromptAssistantService promptAssistantService;
    private final WorkspaceAccessService workspaceAccessService;

    public PromptAssistantController(
            PromptAssistantService promptAssistantService,
            WorkspaceAccessService workspaceAccessService) {
        this.promptAssistantService = promptAssistantService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @PostMapping("/suggestions")
    public ResponseEntity<PromptSuggestionResponse> suggestions(
            @Valid @RequestBody PromptSuggestionRequest request) {
        User user = workspaceAccessService.requireWorkspaceAccessAndGetUser(request.getWorkspaceId());
        promptAssistantService.validateAndRecordUsage(user.getId(), request.getWorkspaceId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(promptAssistantService.generateSuggestions(request));
    }

    @PostMapping("/quality")
    public ResponseEntity<PromptSuggestionResponse> quality(
            @Valid @RequestBody PromptSuggestionRequest request) {
        workspaceAccessService.requireWorkspaceAccess(request.getWorkspaceId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(promptAssistantService.qualityOnly(request));
    }
}
