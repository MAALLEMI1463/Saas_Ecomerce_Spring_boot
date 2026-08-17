package com.example.demo.Service;

import com.example.demo.Dto.CheckoutRequest;
import com.example.demo.Dto.CheckoutSessionResponse;
import com.example.demo.Model.*;
import com.example.demo.Repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ChargilyService chargilyService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        @Lazy ChargilyService chargilyService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.chargilyService = chargilyService;
    }

    // ─── CREATE ORDER — returns checkout URL if ONLINE ───────
    @Transactional
    public OrderResult createOrder(CheckoutRequest request, String subdomain) {

        // 1. Resolve store owner from subdomain
        User storeOwner = userRepository.findBySubdomain(subdomain)
            .orElseThrow(() ->
                new RuntimeException("Store not found: " + subdomain));

        // 2. Build Order shell
        Order order = new Order();
        order.setUser(storeOwner);
        order.setPhone(request.getPhone());
        order.setWilaya(request.getWilaya());
        order.setCommune(request.getCommune());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");

        // 3. Build OrderItems — fetch price from DB only
        List<Long>    productIds = request.getProductIds();
        List<Integer> quantities = request.getQuantities();

        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            int  qty       = (quantities != null && i < quantities.size())
                             ? quantities.get(i) : 1;

            Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                    new RuntimeException("Product not found: " + productId));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setOrder(order);
            item.setQuantity(qty);
            item.setPrice(product.getPrice()); // DB price — never frontend

            total = total.add(
                product.getPrice().multiply(BigDecimal.valueOf(qty)));
            items.add(item);
        }

        order.setTotalPrice(total);
        order.setOrderItems(items);

        // 4. Save order first (we need the ID for Chargily metadata)
        Order savedOrder = orderRepository.save(order);

        // 5. COD → done, no payment gateway needed
        if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            return new OrderResult(savedOrder, null);
        }

        // 6. ONLINE → create Chargily checkout session
        try {
            CheckoutSessionResponse session =
                chargilyService.createCheckoutSession(savedOrder);

            // Store session id as transaction_id
            savedOrder.setTransactionId(session.getId());
            orderRepository.save(savedOrder);

            // Return checkout URL to redirect the user
            return new OrderResult(savedOrder, session.getCheckoutUrl());

        } catch (Exception e) {
            // If Chargily fails, cancel the order
            savedOrder.setStatus("FAILED");
            savedOrder.setPaymentStatus("FAILED");
            orderRepository.save(savedOrder);
            throw new RuntimeException(
                "Payment session creation failed: " + e.getMessage());
        }
    }

    // ─── GET ORDERS BY STATUS ─────────────────────────────────
    public List<Order> getOrdersByStatus(Long userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }

    // ─── UPDATE STATUS ────────────────────────────────────────
    @Transactional
    public void updateOrderStatus(Long orderId, Long userId, String newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                new RuntimeException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied.");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    // ─── INNER RESULT CLASS ───────────────────────────────────
    public static class OrderResult {
        private final Order order;
        private final String checkoutUrl; // null for COD

        public OrderResult(Order order, String checkoutUrl) {
            this.order = order;
            this.checkoutUrl = checkoutUrl;
        }

        public Order getOrder() { return order; }
        public String getCheckoutUrl() { return checkoutUrl; }
        public boolean requiresPayment() { return checkoutUrl != null; }
    }

    // ─── Get order by ID ──────────────────────────────────────
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() ->
                new RuntimeException("Order not found: " + orderId));
    }
}