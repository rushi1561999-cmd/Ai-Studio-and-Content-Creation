package com.example.demo.repository;

import com.example.demo.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, String> {
    List<Asset> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<Asset> findByFolderIdOrderByNameAsc(String folderId);
}
