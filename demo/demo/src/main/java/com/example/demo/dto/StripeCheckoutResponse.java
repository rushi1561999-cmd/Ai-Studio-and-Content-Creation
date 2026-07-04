package com.example.demo.dto;

public class StripeCheckoutResponse {

    private String checkoutUrl;
    private String sessionId;

    public StripeCheckoutResponse() {}

    public StripeCheckoutResponse(String checkoutUrl, String sessionId) {
        this.checkoutUrl = checkoutUrl;
        this.sessionId = sessionId;
    }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
