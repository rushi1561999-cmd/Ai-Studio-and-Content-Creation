package com.example.demo.dto;

public class ContentTypeInfo {
    private String type;
    private String label;
    private String description;
    private int creditCost;

    public ContentTypeInfo() {}

    public ContentTypeInfo(String type, String label, String description, int creditCost) {
        this.type = type;
        this.label = label;
        this.description = description;
        this.creditCost = creditCost;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCreditCost() { return creditCost; }
    public void setCreditCost(int creditCost) { this.creditCost = creditCost; }
}
