package com.example.demo.dto;

public class UpiQrResponse {

    private String upiString;
    private String qrCodeBase64;
    private String amount;
    private String currency;
    private String orderId;
    private String vpa;

    public UpiQrResponse() {}

    public UpiQrResponse(String upiString, String qrCodeBase64, String amount, String currency, String orderId, String vpa) {
        this.upiString = upiString;
        this.qrCodeBase64 = qrCodeBase64;
        this.amount = amount;
        this.currency = currency;
        this.orderId = orderId;
        this.vpa = vpa;
    }

    // Getters and Setters
    public String getUpiString() { return upiString; }
    public void setUpiString(String upiString) { this.upiString = upiString; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }
}
