package com.example.demo.controller;

import com.example.demo.dto.FolderRequest;
import com.example.demo.entity.Asset;
import com.example.demo.entity.AssetVersion;
import com.example.demo.entity.Folder;
import com.example.demo.service.storage.AssetStorageService;
import com.example.demo.service.storage.FolderService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetStorageService assetStorageService;
    private final FolderService folderService;

    public AssetController(AssetStorageService assetStorageService, FolderService folderService) {
        this.assetStorageService = assetStorageService;
        this.folderService = folderService;
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<Asset>> listAssets(@PathVariable String workspaceId) {
        return ResponseEntity.ok(assetStorageService.listWorkspaceAssets(workspaceId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Asset> upload(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String folderId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(assetStorageService.upload(workspaceId, folderId, file));
    }

    @PostMapping(value = "/{assetId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetVersion> addVersion(
            @PathVariable String assetId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(assetStorageService.addVersion(assetId, file));
    }

    @GetMapping("/{assetId}/download")
    public ResponseEntity<Resource> download(@PathVariable String assetId) {
        AssetStorageService.AssetDownload download = assetStorageService.download(assetId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.name(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> delete(@PathVariable String assetId) {
        assetStorageService.delete(assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assetId}/versions")
    public ResponseEntity<List<AssetVersion>> listVersions(@PathVariable String assetId) {
        return ResponseEntity.ok(assetStorageService.listVersions(assetId));
    }

    @GetMapping("/workspace/{workspaceId}/folders")
    public ResponseEntity<List<Folder>> listFolders(@PathVariable String workspaceId) {
        return ResponseEntity.ok(folderService.listFolders(workspaceId));
    }

    @PostMapping("/workspace/{workspaceId}/folders")
    public ResponseEntity<Folder> createFolder(
            @PathVariable String workspaceId,
            @Valid @RequestBody FolderRequest request) {
        return ResponseEntity.ok(folderService.createFolder(workspaceId, request.getName(), request.getParentId()));
    }
}
