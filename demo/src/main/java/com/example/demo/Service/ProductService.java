package com.example.demo.Service;

import com.example.demo.Dto.ProductDTO;
import com.example.demo.Model.Product;
import com.example.demo.Model.User;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository,
                          UserRepository userRepository,
                          FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    // ─── GET: all visible products for current user ──────────
    public List<Product> getVisibleProductsForUser(String username) {
        User user = getUserByUsername(username);
        return productRepository.findByUserIdAndEtatRemove(user.getId(), 1);
    }

    // ─── GET: single product (ownership enforced) ─────────────
    public Product getProductForUser(Long productId, String username) {
        User user = getUserByUsername(username);
        return productRepository.findByIdAndUserId(productId, user.getId())
            .orElseThrow(() -> new RuntimeException("Product not found or access denied."));
    }

    // ─── CREATE ───────────────────────────────────────────────
    public void createProduct(ProductDTO dto, String username) {
        User user = getUserByUsername(username);

        String imageFilename = saveImageIfPresent(dto.getImageFile());

        Product product = new Product();
        product.setUser(user);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImage(imageFilename);
        product.setEtatRemove(1); // visible by default

        productRepository.save(product);
    }

    // ─── UPDATE ───────────────────────────────────────────────
    public void updateProduct(Long productId, ProductDTO dto, String username) {
        Product product = getProductForUser(productId, username);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());

        // Replace image only if a new one is uploaded
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            // Delete old image file
            if (product.getImage() != null) {
                fileStorageService.deleteImage(product.getImage());
            }
            product.setImage(saveImageIfPresent(dto.getImageFile()));
        }

        productRepository.save(product);
    }

    // ─── SOFT DELETE ──────────────────────────────────────────
    public void softDeleteProduct(Long productId, String username) {
        Product product = getProductForUser(productId, username);
        product.setEtatRemove(0); // hide from listing
        productRepository.save(product);
    }

    // ─── HELPERS ──────────────────────────────────────────────
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private String saveImageIfPresent(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            return fileStorageService.saveImage(file);
        }
        return null;
    }
}