package com.example.demo.dto;

public class FolderRequest {
    private String name;
    private String parentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
