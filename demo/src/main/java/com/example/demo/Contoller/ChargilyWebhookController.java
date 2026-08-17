package com.example.demo.Contoller;

import com.example.demo.config.ChargilyConfig;
import com.example.demo.Service.ChargilyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ChargilyWebhookController {

    private final ChargilyService chargilyService;
    private final ChargilyConfig chargilyConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChargilyWebhookController(ChargilyService chargilyService,
                                     ChargilyConfig chargilyConfig) {
        this.chargilyService = chargilyService;
        this.chargilyConfig  = chargilyConfig;
    }

    @PostMapping("/chargily/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Signature", required = false)
                String signature) {

        // ✅ Vérification conditionnelle selon l'environnement
        if (chargilyConfig.isVerifySignature()) {

            // Production : signature obligatoire
            if (signature == null || signature.isBlank()) {
                System.out.println("Webhook rejeté : Signature manquante.");
                return ResponseEntity.status(401)
                    .body("Missing signature.");
            }

            boolean valid = chargilyService
                .isValidSignature(rawBody, signature);

            if (!valid) {
                System.out.println("Webhook rejeté : Signature invalide.");
                return ResponseEntity.status(401)
                    .body("Invalid signature.");
            }

        } else {
            // ✅ Test mode : on skip la vérification
            System.out.println("⚠️ Webhook signature verification DISABLED (test mode).");
        }

        // Parser et traiter le payload
        try {
            Map<String, Object> payload = objectMapper.readValue(
                rawBody,
                new TypeReference<Map<String, Object>>() {}
            );

            chargilyService.handleWebhookEvent(payload);

            return ResponseEntity.ok("Webhook processed.");

        } catch (Exception e) {
            System.out.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(500)
                .body("Error: " + e.getMessage());
        }
    }
}