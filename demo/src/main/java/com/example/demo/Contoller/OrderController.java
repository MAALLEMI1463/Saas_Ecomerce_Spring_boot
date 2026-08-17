package com.example.demo.Contoller;

import com.example.demo.Dto.CheckoutRequest;
import com.example.demo.Model.Order;
import com.example.demo.Service.InvoiceService;
import com.example.demo.Service.OrderService;
import com.example.demo.util.SubdomainResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {

    private final OrderService      orderService;
    private final SubdomainResolver subdomainResolver;
    private final InvoiceService    invoiceService;

    public OrderController(OrderService      orderService,
                           SubdomainResolver subdomainResolver,
                           InvoiceService    invoiceService) {
        this.orderService      = orderService;
        this.subdomainResolver = subdomainResolver;
        this.invoiceService    = invoiceService;
    }

    @GetMapping("/store/cart")
    public String cartPage(HttpServletRequest request,
                           Model model) {
        model.addAttribute("storeName",
            subdomainResolver.extractSubdomain(request));
        return "store/cart";
    }

    @GetMapping("/store/checkout")
    public String checkoutPage(HttpServletRequest request,
                               Model model) {
        model.addAttribute("storeName",
            subdomainResolver.extractSubdomain(request));
        return "store/checkout";
    }

    // ─── STEP 1: Submit Order ─────────────────────────────────
    // Returns JSON: { "type": "COD", "orderId": 42 }
    //            or { "type": "ONLINE", "checkoutUrl": "..." }
    @PostMapping("/store/orders")
    @ResponseBody
    public ResponseEntity<?> submitOrder(
            @ModelAttribute CheckoutRequest request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        String subdomain =
            subdomainResolver.extractSubdomain(httpRequest);

        if (subdomain == null) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", "Invalid store."));
        }

        try {
            OrderService.OrderResult result =
                orderService.createOrder(request, subdomain);

            if (result.requiresPayment()) {
                // ── ONLINE → return Chargily URL ───────────
                return ResponseEntity.ok(java.util.Map.of(
                    "type",        "ONLINE",
                    "checkoutUrl", result.getCheckoutUrl()
                ));
            }

            // ── COD → store orderId in session for PDF ─────
            session.setAttribute(
                "lastOrderId", result.getOrder().getId());

            return ResponseEntity.ok(java.util.Map.of(
                "type",    "COD",
                "orderId", result.getOrder().getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(java.util.Map.of(
                    "error", e.getMessage()));
        }
    }

    // ─── STEP 2: Download Invoice PDF ─────────────────────────
    // Called by JS after order is created
    @GetMapping("/store/invoice/{orderId}")
    public void downloadInvoice(
            @PathVariable Long orderId,
            HttpSession session,
            HttpServletResponse response) throws Exception {

        // Security: verify this order belongs to this session
        Long sessionOrderId =
            (Long) session.getAttribute("lastOrderId");

        if (sessionOrderId == null
                || !sessionOrderId.equals(orderId)) {
            response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied.");
            return;
        }

        // Fetch order and generate PDF
        Order order = orderService.getOrderById(orderId);
        byte[] pdf  = invoiceService.generateInvoice(order);

        String filename = "invoice-"
            + String.format("%05d", orderId) + ".pdf";

        response.setContentType(
            MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();

        // Clear session after download
        session.removeAttribute("lastOrderId");
    }

    // ─── Success + Pending pages ──────────────────────────────
    @GetMapping("/store/order-success")
    public String orderSuccess(HttpServletRequest request,
                               Model model) {
        model.addAttribute("storeName",
            subdomainResolver.extractSubdomain(request));
        return "store/order-success";
    }

    @GetMapping("/store/payment-pending")
    public String paymentPending(HttpServletRequest request,
                                 Model model) {
        model.addAttribute("storeName",
            subdomainResolver.extractSubdomain(request));
        return "store/payment-pending";
    }
}