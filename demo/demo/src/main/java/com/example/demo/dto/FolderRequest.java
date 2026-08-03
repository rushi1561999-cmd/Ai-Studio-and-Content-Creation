package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FolderRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    private String parentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
