package com.example.demo.repository;

import com.example.demo.entity.WorkspaceMember;
import com.example.demo.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {
    // Find all workspaces a user belongs to
    List<WorkspaceMember> findByUserId(String userId);

    boolean existsByUser_IdAndWorkspace_Id(String userId, String workspaceId);

    Optional<WorkspaceMember> findFirstByWorkspace_IdAndRole(String workspaceId, WorkspaceRole role);

    void deleteByUser_Id(String userId);
}
