package com.example.demo.service.storage;

import com.example.demo.entity.Asset;
import com.example.demo.entity.AssetVersion;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AssetVersionRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.audit.AuditService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AssetStorageService {

    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final AuditService auditService;
    private final Path storageRoot;
    private final long maxFileBytes;

    public AssetStorageService(
            AssetRepository assetRepository,
            AssetVersionRepository assetVersionRepository,
            FolderRepository folderRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService,
            AuditService auditService,
            @Value("${app.storage.root:./data/assets}") String storageRoot,
            @Value("${app.storage.max-file-bytes:26214400}") long maxFileBytes) {
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.auditService = auditService;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.maxFileBytes = maxFileBytes;
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Asset storage directory could not be initialized.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<Asset> listWorkspaceAssets(String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return assetRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional
    public Asset upload(String workspaceId, String folderId, MultipartFile file) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        validateFolder(workspaceId, folderId);
        validateFile(file);
        User user = currentUser();

        String displayName = safeDisplayName(file.getOriginalFilename());
        String relativePath = store(workspaceId, displayName, file);
        deleteIfTransactionRollsBack(relativePath);
        try {
            Asset asset = new Asset();
            asset.setWorkspaceId(workspaceId);
            asset.setFolderId(blankToNull(folderId));
            asset.setName(displayName);
            asset.setMimeType(contentType(file));
            asset.setStoragePath(relativePath);
            asset.setSizeBytes(file.getSize());
            asset.setUploadedById(user.getId());
            Asset saved = assetRepository.save(asset);

            AssetVersion version = new AssetVersion();
            version.setAssetId(saved.getId());
            version.setVersionNumber(1);
            version.setStoragePath(relativePath);
            version.setSizeBytes(file.getSize());
            version.setUploadedById(user.getId());
            assetVersionRepository.save(version);

            auditService.log(user.getId(), workspaceId, "ASSET_UPLOADED", "Asset", saved.getId(),
                    "{\"sizeBytes\":" + file.getSize() + "}");
            return saved;
        } catch (RuntimeException exception) {
            deletePhysicalFile(relativePath);
            throw exception;
        }
    }

    @Transactional
    public AssetVersion addVersion(String assetId, MultipartFile file) {
        validateFile(file);
        Asset asset = findAccessibleAsset(assetId);
        User user = currentUser();
        String relativePath = store(asset.getWorkspaceId(), asset.getName(), file);
        deleteIfTransactionRollsBack(relativePath);

        try {
            int nextVersion = assetVersionRepository.findTopByAssetIdOrderByVersionNumberDesc(assetId)
                    .map(version -> version.getVersionNumber() + 1)
                    .orElse(1);

            AssetVersion version = new AssetVersion();
            version.setAssetId(assetId);
            version.setVersionNumber(nextVersion);
            version.setStoragePath(relativePath);
            version.setSizeBytes(file.getSize());
            version.setUploadedById(user.getId());
            AssetVersion saved = assetVersionRepository.save(version);

            asset.setStoragePath(relativePath);
            asset.setSizeBytes(file.getSize());
            asset.setMimeType(contentType(file));
            assetRepository.save(asset);

            auditService.log(user.getId(), asset.getWorkspaceId(), "ASSET_VERSION_UPLOADED", "Asset", assetId,
                    "{\"version\":" + nextVersion + ",\"sizeBytes\":" + file.getSize() + "}");
            return saved;
        } catch (RuntimeException exception) {
            deletePhysicalFile(relativePath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AssetDownload download(String assetId) {
        Asset asset = findAccessibleAsset(assetId);
        Path path = resolveStoredPath(asset.getStoragePath());
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored asset file is missing.");
        }
        return new AssetDownload(
                asset.getName(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                new PathResource(path));
    }

    @Transactional(readOnly = true)
    public List<AssetVersion> listVersions(String assetId) {
        findAccessibleAsset(assetId);
        return assetVersionRepository.findByAssetIdOrderByVersionNumberDesc(assetId);
    }

    @Transactional
    public void delete(String assetId) {
        Asset asset = findAccessibleAsset(assetId);
        User user = currentUser();
        List<AssetVersion> versions = assetVersionRepository.findByAssetIdOrderByVersionNumberDesc(assetId);
        Set<String> storedPaths = new LinkedHashSet<>();
        storedPaths.add(asset.getStoragePath());
        versions.forEach(version -> storedPaths.add(version.getStoragePath()));

        assetVersionRepository.deleteAll(versions);
        assetRepository.delete(asset);
        auditService.log(user.getId(), asset.getWorkspaceId(), "ASSET_DELETED", "Asset", assetId, null);
        deleteAfterCommit(storedPaths);
    }

    private Asset findAccessibleAsset(String assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));
        workspaceAccessService.requireWorkspaceAccess(asset.getWorkspaceId());
        return asset;
    }

    private void validateFolder(String workspaceId, String folderId) {
        if (folderId == null || folderId.isBlank()) {
            return;
        }
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
        if (!workspaceId.equals(folder.getWorkspaceId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Folder belongs to another workspace.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a non-empty file.");
        }
        if (file.getSize() > maxFileBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the configured upload limit.");
        }
    }

    private String store(String workspaceId, String displayName, MultipartFile file) {
        if (!workspaceId.matches("[A-Za-z0-9-]{1,80}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workspace identifier.");
        }
        String extension = extension(displayName);
        String fileName = UUID.randomUUID() + extension;
        String relativePath = workspaceId + "/" + fileName;
        Path target = resolveStoredPath(relativePath);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relativePath;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File could not be stored.");
        }
    }

    private Path resolveStoredPath(String relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path.");
        }
        return resolved;
    }

    private void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredPath(relativePath));
        } catch (IOException ignored) {
            // The database deletion remains authoritative; operations can reconcile an orphaned file.
        }
    }

    private void deleteIfTransactionRollsBack(String relativePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deletePhysicalFile(relativePath);
                }
            }
        });
    }

    private void deleteAfterCommit(Set<String> storedPaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storedPaths.forEach(this::deletePhysicalFile);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storedPaths.forEach(AssetStorageService.this::deletePhysicalFile);
            }
        });
    }

    private String safeDisplayName(String originalName) {
        String candidate = originalName == null ? "asset" : originalName.replace('\\', '/');
        candidate = candidate.substring(candidate.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (candidate.isBlank() || ".".equals(candidate) || "..".equals(candidate)) {
            candidate = "asset";
        }
        return candidate.length() > 255 ? candidate.substring(0, 255) : candidate;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        String value = name.substring(dot).toLowerCase();
        return value.matches("\\.[a-z0-9]{1,10}") ? value : "";
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private User currentUser() {
        String email = workspaceAccessService.currentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    public record AssetDownload(String name, String contentType, long sizeBytes, Resource resource) {}
}
