package com.example.demo.Service;

import com.example.demo.Model.Product;
import com.example.demo.Model.User;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StoreService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public StoreService(UserRepository userRepository,
                        ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    // Find store owner by subdomain
    public Optional<User> findUserBySubdomain(String subdomain) {
        return userRepository.findBySubdomain(subdomain);
    }

    // Get all visible products for a store
    public List<Product> getStoreProducts(Long userId) {
        return productRepository.findByUserIdAndEtatRemove(userId, 1);
    }

    // Search products by name within a store
    public List<Product> searchStoreProducts(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getStoreProducts(userId);
        }
        return productRepository
            .findByUserIdAndEtatRemoveAndNameContainingIgnoreCase(
                userId, 1, keyword.trim());
    }

    // Get single product details (public, only checks visibility)
    public Optional<Product> getProductDetail(Long productId, Long userId) {
        return productRepository.findByIdAndUserId(productId, userId)
            .filter(p -> p.getEtatRemove() == 1);
    }
}