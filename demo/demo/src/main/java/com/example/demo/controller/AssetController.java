package com.example.demo.controller;

import com.example.demo.dto.AssetRequest;
import com.example.demo.dto.FolderRequest;
import com.example.demo.entity.Asset;
import com.example.demo.entity.AssetVersion;
import com.example.demo.entity.Folder;
import com.example.demo.service.storage.AssetStorageService;
import com.example.demo.service.storage.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<Asset> registerAsset(@RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetStorageService.registerAsset(
                request.getWorkspaceId(),
                request.getFolderId(),
                request.getName(),
                request.getMimeType(),
                request.getSizeBytes()));
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
            @RequestBody FolderRequest request) {
        return ResponseEntity.ok(folderService.createFolder(workspaceId, request.getName(), request.getParentId()));
    }
}
