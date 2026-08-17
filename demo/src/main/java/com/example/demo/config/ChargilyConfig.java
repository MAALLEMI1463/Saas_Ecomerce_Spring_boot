package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChargilyConfig {

    @Value("${chargily.api.key}")
    private String apiKey;

    @Value("${chargily.webhook.secret}")
    private String webhookSecret;

    @Value("${chargily.api.url}")
    private String apiUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    // ✅ Nouvelle propriété pour activer/désactiver la vérification
    @Value("${chargily.webhook.verify-signature:false}")
    private boolean verifySignature;

    public String getApiKey() { return apiKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public String getApiUrl() { return apiUrl; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isVerifySignature() { return verifySignature; }
}