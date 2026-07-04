package com.example.demo.dto;

public class AssetRequest {
    private String workspaceId;
    private String folderId;
    private String name;
    private String mimeType;
    private long sizeBytes;

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
