package com.example.demo.dto;

public class RazorpayCheckoutResponse {

    private String orderId;
    private String key;
    private String amount;
    private String currency;
    private String email;
    private String name;
    private String description;
    private String theme;
    private String image;
    private String prefill;

    public RazorpayCheckoutResponse() {}

    public RazorpayCheckoutResponse(String orderId, String key, String amount, String currency, 
                                     String email, String name, String description) {
        this.orderId = orderId;
        this.key = key;
        this.amount = amount;
        this.currency = currency;
        this.email = email;
        this.name = name;
        this.description = description;
        this.theme = "#3b82f6";
        this.image = "/logo.png";
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getPrefill() { return prefill; }
    public void setPrefill(String prefill) { this.prefill = prefill; }
}
