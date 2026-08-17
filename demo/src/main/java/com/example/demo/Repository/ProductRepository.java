package com.example.demo.Repository;

import com.example.demo.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Only visible products for a specific user
    List<Product> findByUserIdAndEtatRemove(Long userId, Integer etatRemove);

    // Find product by id and user id (ownership check)
    Optional<Product> findByIdAndUserId(Long id, Long userId);

    // For store: visible products + search by name
    List<Product> findByUserIdAndEtatRemoveAndNameContainingIgnoreCase(
        Long userId, Integer etatRemove, String name);

    // ✅ Count products for a specific user
    long countByUserId(Long userId);
}
