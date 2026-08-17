package com.example.demo.Contoller;

import com.example.demo.Model.Order;
import com.example.demo.Model.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
public class OrderManagementController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderManagementController(OrderService orderService,
                                     UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    // ─── PENDING ORDERS ───────────────────────────────────────
    @GetMapping("/pending")
    public String pendingOrders(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = resolveUser(userDetails);
        List<Order> orders = orderService.getOrdersByStatus(user.getId(), "PENDING");
        model.addAttribute("orders", orders);
        model.addAttribute("activeTab", "pending");
        return "orders/pending";
    }

    // ─── CONFIRMED ORDERS ─────────────────────────────────────
    @GetMapping("/confirmed")
    public String confirmedOrders(@AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User user = resolveUser(userDetails);
        List<Order> orders = orderService.getOrdersByStatus(user.getId(), "CONFIRMED");
        model.addAttribute("orders", orders);
        model.addAttribute("activeTab", "confirmed");
        return "orders/confirmed";
    }

    // ─── LIVRED ORDERS ────────────────────────────────────────
    @GetMapping("/livred")
    public String livredOrders(@AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User user = resolveUser(userDetails);
        List<Order> orders = orderService.getOrdersByStatus(user.getId(), "LIVRED");
        model.addAttribute("orders", orders);
        model.addAttribute("activeTab", "livred");
        return "orders/livred";
    }

    // ─── CONFIRM ORDER (PENDING → CONFIRMED) ──────────────────
    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        orderService.updateOrderStatus(id, user.getId(), "CONFIRMED");
        return "redirect:/user/orders/pending";
    }

    // ─── MARK AS LIVRED (CONFIRMED → LIVRED) ──────────────────
    @PostMapping("/{id}/livred")
    public String markLivred(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        orderService.updateOrderStatus(id, user.getId(), "LIVRED");
        return "redirect:/user/orders/confirmed";
    }

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found"));
    }
}