package com.example.demo.service.storage;

import com.example.demo.entity.Asset;
import com.example.demo.entity.AssetVersion;
import com.example.demo.entity.User;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AssetVersionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AssetStorageService {

    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final AuditService auditService;

    public AssetStorageService(
            AssetRepository assetRepository,
            AssetVersionRepository assetVersionRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService,
            AuditService auditService) {
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Asset> listWorkspaceAssets(String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return assetRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional
    public Asset registerAsset(String workspaceId, String folderId, String name, String mimeType, long sizeBytes) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        User user = currentUser();

        String storagePath = "workspaces/" + workspaceId + "/assets/" + UUID.randomUUID();

        Asset asset = new Asset();
        asset.setWorkspaceId(workspaceId);
        asset.setFolderId(folderId);
        asset.setName(name);
        asset.setMimeType(mimeType);
        asset.setStoragePath(storagePath);
        asset.setSizeBytes(sizeBytes);
        asset.setUploadedById(user.getId());
        Asset saved = assetRepository.save(asset);

        AssetVersion version = new AssetVersion();
        version.setAssetId(saved.getId());
        version.setVersionNumber(1);
        version.setStoragePath(storagePath);
        version.setSizeBytes(sizeBytes);
        version.setUploadedById(user.getId());
        assetVersionRepository.save(version);

        auditService.log(user.getId(), workspaceId, "ASSET_CREATED", "Asset", saved.getId(), null);
        return saved;
    }

    @Transactional
    public AssetVersion addVersion(String assetId, String storagePath, long sizeBytes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));
        workspaceAccessService.requireWorkspaceAccess(asset.getWorkspaceId());
        User user = currentUser();

        int nextVersion = assetVersionRepository.findTopByAssetIdOrderByVersionNumberDesc(assetId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        AssetVersion version = new AssetVersion();
        version.setAssetId(assetId);
        version.setVersionNumber(nextVersion);
        version.setStoragePath(storagePath);
        version.setSizeBytes(sizeBytes);
        version.setUploadedById(user.getId());
        AssetVersion saved = assetVersionRepository.save(version);

        asset.setStoragePath(storagePath);
        asset.setSizeBytes(sizeBytes);
        assetRepository.save(asset);

        auditService.log(user.getId(), asset.getWorkspaceId(), "ASSET_VERSION_ADDED", "Asset", assetId,
                "{\"version\":" + nextVersion + "}");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AssetVersion> listVersions(String assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));
        workspaceAccessService.requireWorkspaceAccess(asset.getWorkspaceId());
        return assetVersionRepository.findByAssetIdOrderByVersionNumberDesc(assetId);
    }

    private User currentUser() {
        String email = workspaceAccessService.currentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
