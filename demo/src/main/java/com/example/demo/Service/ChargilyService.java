package com.example.demo.Service;

import com.example.demo.config.ChargilyConfig;
import com.example.demo.Dto.CheckoutSessionResponse;
import com.example.demo.Model.Order;
import com.example.demo.Repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class ChargilyService {

    private final ChargilyConfig config;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ChargilyService(ChargilyConfig config,
                           OrderRepository orderRepository) {
        this.config = config;
        this.orderRepository = orderRepository;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    // ─── CREATE CHECKOUT SESSION ──────────────────────────────
    public CheckoutSessionResponse createCheckoutSession(Order order) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("order_id", order.getId().toString());

            Map<String, Object> body = new HashMap<>();
            double amountInCentimes = order.getTotalPrice()
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            body.put("amount", amountInCentimes);
            body.put("currency", "dzd");
            body.put("success_url",
                config.getBaseUrl() + "/store/payment-pending");
            body.put("failure_url",
                config.getBaseUrl() + "/store/payment-pending");
            body.put("metadata", metadata);

            String jsonBody = objectMapper.writeValueAsString(body);
            String apiKey   = config.getApiKey().trim();
            String url      = config.getApiUrl().trim() + "/checkouts";

            System.out.println("=== Chargily Request ===");
            System.out.println("URL  : " + url);
            System.out.println("Body : " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "SaasApp/1.0")
                .POST(HttpRequest.BodyPublishers
                    .ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

            System.out.println("Status : " + response.statusCode());
            System.out.println("Body   : " + response.body());
            System.out.println("========================");

            if (response.statusCode() != 200
                    && response.statusCode() != 201) {
                throw new RuntimeException(
                    "Chargily API error "
                    + response.statusCode()
                    + " : " + response.body());
            }

            return objectMapper.readValue(
                response.body(),
                CheckoutSessionResponse.class);

        } catch (java.net.UnknownHostException e) {
            throw new RuntimeException(
                "DNS Error: Cannot resolve 'pay.chargily.net'.", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create checkout session: "
                + e.getMessage(), e);
        }
    }

    // ─── VERIFY WEBHOOK SIGNATURE ─────────────────────────────
    // ✅ دالة مُعادة — كانت محذوفة بالخطأ
    public boolean isValidSignature(String rawBody,
                                    String signatureHeader) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                config.getWebhookSecret()
                      .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(
                rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            System.out.println("Signature computed : " + computed);
            System.out.println("Signature received : " + signatureHeader);

            return computed.equalsIgnoreCase(signatureHeader);

        } catch (Exception e) {
            throw new RuntimeException(
                "Signature verification failed.", e);
        }
    }

    // ─── HANDLE WEBHOOK EVENT ─────────────────────────────────
    public void handleWebhookEvent(Map<String, Object> payload) {

        String type = (String) payload.get("type");

        @SuppressWarnings("unchecked")
        Map<String, Object> data =
            (Map<String, Object>) payload.get("data");

        if (data == null) {
            System.out.println("Webhook: no data field.");
            return;
        }

        String checkoutId = (String) data.get("id");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata =
            (Map<String, Object>) data.get("metadata");

        String orderId = null;
        if (metadata != null) {
            orderId = (String) metadata.get("order_id");
        }

        System.out.println("=== Webhook Event ===");
        System.out.println("Type       : " + type);
        System.out.println("CheckoutId : " + checkoutId);
        System.out.println("OrderId    : " + orderId);
        System.out.println("=====================");

        Optional<Order> orderOpt = Optional.empty();

        if (orderId != null) {
            try {
                orderOpt = orderRepository
                    .findById(Long.parseLong(orderId));
            } catch (NumberFormatException ignored) {}
        }

        if (orderOpt.isEmpty() && checkoutId != null) {
            orderOpt = orderRepository
                .findByTransactionId(checkoutId);
        }

        if (orderOpt.isEmpty()) {
            System.out.println("Webhook: order not found.");
            return;
        }

        Order order = orderOpt.get();

        switch (type) {
            case "checkout.paid" -> {
                order.setPaymentStatus("PAID");
                order.setStatus("CONFIRMED");
                System.out.println("✅ Order #"
                    + order.getId() + " → PAID / CONFIRMED");
            }
            case "checkout.failed" -> {
                order.setPaymentStatus("FAILED");
                System.out.println("❌ Order #"
                    + order.getId() + " → FAILED");
            }
            default ->
                System.out.println("Unhandled event: " + type);
        }

        orderRepository.save(order);
    }
}