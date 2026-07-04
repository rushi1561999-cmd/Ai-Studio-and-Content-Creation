package com.example.demo.controller;

import com.example.demo.dto.PromptRequest;
import com.example.demo.dto.PromptResponse;
import com.example.demo.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping
    public ResponseEntity<PromptResponse> createPrompt(@RequestBody PromptRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName(); 
        
        return ResponseEntity.ok(promptService.createPrompt(request, userEmail));
    } // <-- THIS WAS THE MISSING BRACKET!

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<PromptResponse>> getPromptsForWorkspace(@PathVariable String workspaceId) {
        // The @PathVariable extracts the ID right out of the URL!
        return ResponseEntity.ok(promptService.getPromptsByWorkspace(workspaceId));
    }
}
