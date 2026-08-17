package com.example.demo.Repository;

import com.example.demo.Model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    // ✅ Top 5 most ordered products for a specific user
    // Join: order_items → products → filter by product.user_id
    @Query(value = """
        SELECT
            p.name          AS productName,
            SUM(oi.quantity) AS totalQuantity
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        WHERE p.user_id = :userId
        GROUP BY p.id, p.name
        ORDER BY totalQuantity DESC
        LIMIT 5
        """,
        nativeQuery = true)
    List<Object[]> findTopProductsByUserId(
        @Param("userId") Long userId);
}