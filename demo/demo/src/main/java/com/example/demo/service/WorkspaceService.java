package com.example.demo.service;

import com.example.demo.dto.WorkspaceRequest;

import com.example.demo.dto.WorkspaceResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.Workspace;
import com.example.demo.entity.WorkspaceMember;
import com.example.demo.entity.WorkspaceRole;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkspaceMemberRepository;
import com.example.demo.repository.WorkspaceRepository;
import com.example.demo.service.billing.WalletBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final WalletBillingService walletBillingService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            UserRepository userRepository,
                            WalletBillingService walletBillingService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.walletBillingService = walletBillingService;
    }

    // @Transactional ensures that if either save fails, the whole operation rolls back
    @Transactional
    public WorkspaceResponse createWorkspace(WorkspaceRequest request, String userEmail) {
        
        // 1. Find the user making the request
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        // 2. Create and save the new Workspace
        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        Workspace savedWorkspace = workspaceRepository.saveAndFlush(workspace);

        // 3. Add the creator as the OWNER
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(savedWorkspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        walletBillingService.getOrCreateWallet(savedWorkspace.getId());

        // 4. Build the response
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(savedWorkspace.getId());
        response.setName(savedWorkspace.getName());
        response.setRole(Role.OWNER.name());
        response.setCreatedAt(savedWorkspace.getCreatedAt());

        return response;
    }

    public List<WorkspaceResponse> getUserWorkspaces(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());

        return memberships.stream().map(member -> {
            WorkspaceResponse response = new WorkspaceResponse();
            response.setId(member.getWorkspace().getId());
            response.setName(member.getWorkspace().getName());
            response.setRole(member.getRole().name());
            response.setCreatedAt(member.getWorkspace().getCreatedAt());
            return response;
        }).collect(Collectors.toList());
    }
}