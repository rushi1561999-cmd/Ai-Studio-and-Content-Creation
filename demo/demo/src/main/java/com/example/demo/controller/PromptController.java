package com.example.demo.controller;

import com.example.demo.dto.PromptRequest;
import com.example.demo.dto.PromptResponse;
import com.example.demo.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<PromptResponse>> getPromptsForWorkspace(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(promptService.getPromptsByWorkspace(
                workspaceId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
