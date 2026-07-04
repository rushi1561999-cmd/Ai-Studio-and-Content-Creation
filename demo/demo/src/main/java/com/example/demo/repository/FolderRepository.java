package com.example.demo.repository;

import com.example.demo.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, String> {
    List<Folder> findByWorkspaceIdOrderByNameAsc(String workspaceId);
    List<Folder> findByWorkspaceIdAndParentId(String workspaceId, String parentId);
}
