package com.example.demo.Contoller;

import com.example.demo.Dto.DashboardStats;
import com.example.demo.Model.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Service.DashboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository   userRepository; // ✅ ADD
    private final ObjectMapper     objectMapper;

    public DashboardController(DashboardService dashboardService, UserRepository   userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository   = userRepository;
        this.objectMapper     = new ObjectMapper();
    }

    // ─── ADMIN dashboard ──────────────────────────────────────
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String registered,
            Model model) {
        model.addAttribute("username", userDetails.getUsername());
        if (registered != null) {
            model.addAttribute("successMsg",
                "User registered successfully!");
        }
        return "admin-dashboard";
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws JsonProcessingException {

        String username = userDetails.getUsername();

        // Fetch full user from DB
        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "User not found: " + username));

        // ✅ Generate initials from username
        // e.g. "madjid" → "MA", "john_doe" → "JD"
        String initials = generateInitials(username);

        // 1. Stats cards
        DashboardStats stats =
            dashboardService.getStatsForUser(username);

        // 2. Line chart — monthly revenue
        Map<String, Double> monthlyRevenue =
            dashboardService.getMonthlyRevenueForUser(username);
        String chartLabels = objectMapper.writeValueAsString(
            new ArrayList<>(monthlyRevenue.keySet()));
        String chartValues = objectMapper.writeValueAsString(
            new ArrayList<>(monthlyRevenue.values()));

        // 3. Pie chart — orders by status
        String pieLabels = objectMapper.writeValueAsString(
            List.of("Pending", "Confirmed", "Delivered"));
        String pieValues = objectMapper.writeValueAsString(
            List.of(
                stats.getPendingOrders(),
                stats.getConfirmedOrders(),
                stats.getLivredOrders()
            ));

        // 4. Line chart — monthly orders count
        Map<String, Long> monthlyOrders =
            dashboardService.getMonthlyOrderCountForUser(username);
        String ordersChartLabels = objectMapper.writeValueAsString(
            new ArrayList<>(monthlyOrders.keySet()));
        String ordersChartValues = objectMapper.writeValueAsString(
            new ArrayList<>(monthlyOrders.values()));

        // ✅ 5. Bar chart — top 5 products
        Map<String, Long> topProducts =
            dashboardService.getTopProductsForUser(username);
        String topProductsLabels = objectMapper.writeValueAsString(
            new ArrayList<>(topProducts.keySet()));
        String topProductsValues = objectMapper.writeValueAsString(
            new ArrayList<>(topProducts.values()));

        model.addAttribute("userInitials", initials);        // ✅
        model.addAttribute("userRole",    user.getRole());   // ✅
        model.addAttribute("subdomain",   user.getSubdomain()); // ✅
        model.addAttribute("username",           username);
        model.addAttribute("stats",              stats);
        model.addAttribute("chartLabels",        chartLabels);
        model.addAttribute("chartValues",        chartValues);
        model.addAttribute("pieLabels",          pieLabels);
        model.addAttribute("pieValues",          pieValues);
        model.addAttribute("ordersChartLabels",  ordersChartLabels);
        model.addAttribute("ordersChartValues",  ordersChartValues);
        model.addAttribute("topProductsLabels",  topProductsLabels); // ✅
        model.addAttribute("topProductsValues",  topProductsValues); // ✅

        return "user-dashboard";
    }

    // ─── Helper: generate initials from username ──────────────
    private String generateInitials(String username) {
        if (username == null || username.isBlank()) return "??";

        // Split by underscore, dot, space, or hyphen
        String[] parts = username.split("[_\\.\\-\\s]+");

        if (parts.length >= 2) {
            // "john_doe" → "JD"
            return (parts[0].substring(0, 1)
                + parts[1].substring(0, 1))
                .toUpperCase();
        }

        // "madjid" → "MA"
        return username.substring(0,
            Math.min(2, username.length())).toUpperCase();
    }
}