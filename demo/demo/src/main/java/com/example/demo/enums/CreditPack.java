package com.example.demo.enums;

public enum CreditPack {
    STARTER("starter", 100, 900L, "Starter — 100 AI credits"),
    PROFESSIONAL("professional", 500, 2900L, "Professional — 500 AI credits"),
    ENTERPRISE("enterprise", 1000, 9900L, "Enterprise — 1000 AI credits");

    private final String id;
    private final int credits;
    private final long priceCents;
    private final String label;

    CreditPack(String id, int credits, long priceCents, String label) {
        this.id = id;
        this.credits = credits;
        this.priceCents = priceCents;
        this.label = label;
    }

    public String getId() { return id; }
    public int getCredits() { return credits; }
    public long getPriceCents() { return priceCents; }
    public String getLabel() { return label; }

    public static CreditPack fromId(String id) {
        for (CreditPack pack : values()) {
            if (pack.id.equalsIgnoreCase(id)) {
                return pack;
            }
        }
        throw new IllegalArgumentException("Unknown credit pack: " + id);
    }
}
