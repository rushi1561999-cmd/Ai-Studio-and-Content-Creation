package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_versions")
public class AssetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "asset_id", nullable = false)
    private String assetId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(name = "uploaded_by_id")
    private String uploadedById;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getUploadedById() { return uploadedById; }
    public void setUploadedById(String uploadedById) { this.uploadedById = uploadedById; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
