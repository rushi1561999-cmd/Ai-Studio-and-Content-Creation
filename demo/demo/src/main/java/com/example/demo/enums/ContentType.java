package com.example.demo.enums;

public enum ContentType {
    TEXT(1),
    IMAGE(3),
    VIDEO(10),
    MIXED(5);

    private final int creditCost;

    ContentType(int creditCost) {
        this.creditCost = creditCost;
    }

    public int getCreditCost() {
        return creditCost;
    }

    public static ContentType fromString(String value) {
        if (value == null || value.isBlank()) {
            return TEXT;
        }
        return ContentType.valueOf(value.trim().toUpperCase());
    }
}
