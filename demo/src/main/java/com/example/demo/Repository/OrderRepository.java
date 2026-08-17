package com.example.demo.Repository;

import com.example.demo.Dto.MonthlyRevenueDTO;
import com.example.demo.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdAndStatus(Long userId, String status);

    List<Order> findByUserId(Long userId);

    // ← ADD THIS for webhook fallback lookup
    Optional<Order> findByTransactionId(String transactionId);

        // ✅ Count orders by user + status
    long countByUserIdAndStatus(Long userId, String status);

    // ✅ Total revenue: sum(total_price)
    //    where user_id = :userId
    //    AND (status = 'LIVRED' OR payment_status = 'PAID')
    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0)
        FROM Order o
        WHERE o.user.id = :userId
        AND (o.status = 'LIVRED' OR o.paymentStatus = 'PAID')
    """)
    BigDecimal sumRevenueByUserId(@Param("userId") Long userId);

    // ✅ Native SQL — évite le problème Hibernate 7 avec SELECT new
    // Retourne List<Object[]> : [0]=yearMonth String, [1]=revenue Double
    @Query(value = """
        SELECT
            CONCAT(YEAR(created_at), '-',
                   LPAD(MONTH(created_at), 2, '0')) AS yearMonth,
            SUM(total_price) AS revenue
        FROM orders
        WHERE user_id = :userId
          AND (status = 'LIVRED' OR payment_status = 'PAID')
        GROUP BY YEAR(created_at), MONTH(created_at)
        ORDER BY YEAR(created_at) ASC, MONTH(created_at) ASC
        """,
        nativeQuery = true)
    List<Object[]> findMonthlyRevenueByUserId(
        @Param("userId") Long userId);

    // ✅ Count orders per month for current user (Native SQL)
    @Query(value = """
        SELECT
            CONCAT(YEAR(created_at), '-',
                LPAD(MONTH(created_at), 2, '0')) AS yearMonth,
            COUNT(*) AS orderCount
        FROM orders
        WHERE user_id = :userId
        GROUP BY YEAR(created_at), MONTH(created_at)
        ORDER BY YEAR(created_at) ASC, MONTH(created_at) ASC
        """,
        nativeQuery = true)
    List<Object[]> findMonthlyOrderCountByUserId(
        @Param("userId") Long userId);

}