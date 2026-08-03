package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceAccessService {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceAccessService(
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        return auth.getName();
    }

    public void requireWorkspaceAccess(String workspaceId) {
        requireWorkspaceAccessAndGetUser(workspaceId);
    }

    public User requireWorkspaceAccessAndGetUser(String workspaceId) {
        User user = currentUser();
        if (!workspaceMemberRepository.existsByUser_IdAndWorkspace_Id(user.getId(), workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this workspace.");
        }
        return user;
    }

    public User currentUser() {
        return userRepository.findByEmail(currentUserEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    public boolean isAdmin() {
        User user = currentUser();
        return user.isAdmin();
    }
}