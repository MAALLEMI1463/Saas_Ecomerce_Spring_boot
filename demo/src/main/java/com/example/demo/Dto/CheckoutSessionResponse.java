package com.example.demo.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// ✅ تجاهل أي حقل غير معرّف يأتي من Chargily API
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutSessionResponse {

    private String id;

    @JsonProperty("checkout_url")
    private String checkoutUrl;

    private String status;

    public CheckoutSessionResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}