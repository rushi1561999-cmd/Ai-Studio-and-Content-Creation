package com.example.demo.controller;

import com.example.demo.dto.WorkspaceRequest;
import com.example.demo.dto.WorkspaceResponse;
import com.example.demo.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody WorkspaceRequest request) {
        // Extract the email of the currently authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName(); 

        return ResponseEntity.ok(workspaceService.createWorkspace(request, userEmail));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        
        return ResponseEntity.ok(workspaceService.getUserWorkspaces(userEmail));
    }
}