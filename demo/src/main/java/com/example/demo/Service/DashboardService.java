package com.example.demo.Service;

import com.example.demo.Dto.DashboardStats;
import com.example.demo.Model.User;
import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;
    private final OrderItemRepository orderItemRepository; // ✅ ADD

    public DashboardService(ProductRepository productRepository,
                            OrderRepository   orderRepository,
                            UserRepository    userRepository,
                            OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderRepository   = orderRepository;
        this.userRepository    = userRepository;
        this.orderItemRepository = orderItemRepository; 
    }

    // ─── Resolve user ─────────────────────────────────────────
    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "User not found: " + username));
    }

    // ─── Stats cards ──────────────────────────────────────────
    public DashboardStats getStatsForUser(String username) {
        Long userId = resolveUser(username).getId();

        long totalProducts =
            productRepository.countByUserId(userId);
        long pendingOrders =
            orderRepository.countByUserIdAndStatus(userId, "PENDING");
        long confirmedOrders =
            orderRepository.countByUserIdAndStatus(userId, "CONFIRMED");
        long livredOrders =
            orderRepository.countByUserIdAndStatus(userId, "LIVRED");

        BigDecimal totalRevenue =
            orderRepository.sumRevenueByUserId(userId);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        return new DashboardStats(
            totalProducts, pendingOrders,
            confirmedOrders, livredOrders, totalRevenue);
    }

    // ─── Chart data: monthly revenue ─────────────────────────
    public Map<String, Double> getMonthlyRevenueForUser(
            String username) {

        Long userId = resolveUser(username).getId();

        // ✅ Native query returns List<Object[]>
        // row[0] = "2025-01" (String)
        // row[1] = 45000.00  (BigDecimal from MySQL SUM)
        List<Object[]> rows =
            orderRepository.findMonthlyRevenueByUserId(userId);

        Map<String, Double> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String yearMonth = (String) row[0];
            Double revenue   = 0.0;

            if (row[1] != null) {
                // MySQL SUM returns BigDecimal
                if (row[1] instanceof BigDecimal bd) {
                    revenue = bd.doubleValue();
                } else if (row[1] instanceof Double d) {
                    revenue = d;
                } else if (row[1] instanceof Number n) {
                    revenue = n.doubleValue();
                }
            }

            // Format "2025-01" → "Jan 2025"
            result.put(formatMonthLabel(yearMonth), revenue);
        }

        return result;
    }

    // ─── Helper: "2025-01" → "Jan 2025" ──────────────────────
    private String formatMonthLabel(String yearMonth) {
        if (yearMonth == null || !yearMonth.contains("-"))
            return yearMonth;

        String[] parts = yearMonth.split("-");
        if (parts.length < 2) return yearMonth;

        String[] months = {
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        try {
            int monthNum = Integer.parseInt(parts[1]);
            String name  = (monthNum >= 1 && monthNum <= 12)
                ? months[monthNum] : parts[1];
            return name + " " + parts[0]; // "Jan 2025"
        } catch (NumberFormatException e) {
            return yearMonth;
        }
    }

    // ─── Chart data: monthly orders count ────────────────────
    public Map<String, Long> getMonthlyOrderCountForUser(
            String username) {

        Long userId = resolveUser(username).getId();

        List<Object[]> rows =
            orderRepository.findMonthlyOrderCountByUserId(userId);

        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String yearMonth = (String) row[0];
            Long   count     = 0L;

            if (row[1] != null) {
                if (row[1] instanceof Long l) {
                    count = l;
                } else if (row[1] instanceof Number n) {
                    count = n.longValue();
                }
            }

            result.put(formatMonthLabel(yearMonth), count);
        }

        return result;
    }

    // ─── Chart data: top 5 products by quantity ───────────────
    public Map<String, Long> getTopProductsForUser(String username) {

        Long userId = resolveUser(username).getId();

        List<Object[]> rows =
            orderItemRepository.findTopProductsByUserId(userId);

        // LinkedHashMap preserves DESC order from SQL
        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String productName = (String) row[0];
            Long   quantity    = 0L;

            if (row[1] != null) {
                if (row[1] instanceof Long l) {
                    quantity = l;
                } else if (row[1] instanceof Number n) {
                    quantity = n.longValue();
                }
            }

            result.put(productName, quantity);
        }

        return result;
    }
}