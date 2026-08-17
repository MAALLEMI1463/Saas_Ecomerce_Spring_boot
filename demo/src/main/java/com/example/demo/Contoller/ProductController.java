package com.example.demo.Contoller;

import com.example.demo.Dto.ProductDTO;
import com.example.demo.Model.Product;
import com.example.demo.Service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─── LIST: show only user's visible products ──────────────
    @GetMapping
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        List<Product> products = productService
            .getVisibleProductsForUser(userDetails.getUsername());
        model.addAttribute("products", products);
        return "products/list";
    }

    // ─── ADD: show form ───────────────────────────────────────
    @GetMapping("/add")
    public String addProductForm(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        return "products/add";
    }

    // ─── ADD: submit form ────────────────────────────────────
    @PostMapping("/add")
    public String addProductSubmit(
            @Valid @ModelAttribute("productDTO") ProductDTO dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (dto.getImageFile() == null || dto.getImageFile().isEmpty()) {
            result.rejectValue("imageFile", "required", "Image is required");
        }

        if (result.hasErrors()) {
            return "products/add";
        }

        productService.createProduct(dto, userDetails.getUsername());
        return "redirect:/products?added=true";
    }

    // ─── DETAILS ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        Product product = productService.getProductForUser(id, userDetails.getUsername());
        model.addAttribute("product", product);
        return "products/details";
    }

    // ─── EDIT: show pre-filled form ───────────────────────────
    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Product product = productService.getProductForUser(id, userDetails.getUsername());

        // Pre-fill DTO with existing data
        ProductDTO dto = new ProductDTO();
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());

        model.addAttribute("productDTO", dto);
        model.addAttribute("product", product);
        return "products/edit";
    }

    // ─── EDIT: submit update ──────────────────────────────────
    @PostMapping("/{id}/edit")
    public String editProductSubmit(
            @PathVariable Long id,
            @Valid @ModelAttribute("productDTO") ProductDTO dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (result.hasErrors()) {
            // Keep existing product for image display
            Product product = productService.getProductForUser(id, userDetails.getUsername());
            model.addAttribute("product", product);
            return "products/edit";
        }

        productService.updateProduct(id, dto, userDetails.getUsername());
        return "redirect:/products?updated=true";
    }

    // ─── SOFT DELETE ──────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        productService.softDeleteProduct(id, userDetails.getUsername());
        return "redirect:/products?deleted=true";
    }

    
}
