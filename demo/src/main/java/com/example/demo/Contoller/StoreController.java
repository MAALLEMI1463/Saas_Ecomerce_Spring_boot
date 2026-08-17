package com.example.demo.Contoller;

import com.example.demo.Model.Product;
import com.example.demo.Model.User;
import com.example.demo.Service.StoreService;
import com.example.demo.util.SubdomainResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class StoreController {

    private final StoreService storeService;
    private final SubdomainResolver subdomainResolver;

    public StoreController(StoreService storeService,
                           SubdomainResolver subdomainResolver) {
        this.storeService = storeService;
        this.subdomainResolver = subdomainResolver;
    }

    // ─── STORE HOME PAGE ─────────────────────────────────────
    @GetMapping("/store/home")
    public String storeHome(
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request,
            Model model) {

        String subdomain = subdomainResolver.extractSubdomain(request);

        // Guard: if someone hits /store/home on plain localhost, send them to login
        if (subdomain == null) {
            return "redirect:/login";
        }

        Optional<User> storeOwner = storeService.findUserBySubdomain(subdomain);

        if (storeOwner.isEmpty()) {
            model.addAttribute("errorMsg",
                "Store \"" + subdomain + "\" does not exist.");
            return "store/not-found";
        }

        User user = storeOwner.get();
        List<Product> products = storeService.searchStoreProducts(user.getId(), search);

        model.addAttribute("storeName", user.getSubdomain());
        model.addAttribute("products", products);
        model.addAttribute("search", search != null ? search : "");

        return "store/home";
    }

    // ─── PRODUCT DETAILS (public) ─────────────────────────────
    @GetMapping("/store/product/{id}")
    public String productDetail(
            @PathVariable Long id,
            HttpServletRequest request,
            Model model) {

        String subdomain = subdomainResolver.extractSubdomain(request);

        if (subdomain == null) return "redirect:/login";

        Optional<User> storeOwner = storeService.findUserBySubdomain(subdomain);
        if (storeOwner.isEmpty()) return "store/not-found";

        User user = storeOwner.get();

        Optional<Product> productOpt =
            storeService.getProductDetail(id, user.getId());

        if (productOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Product not found.");
            return "store/not-found";
        }

        model.addAttribute("product", productOpt.get());
        model.addAttribute("storeName", user.getSubdomain());

        return "store/product-detail";
    }

    // ─── CART (UI only — no logic yet) ───────────────────────
    // @GetMapping("/store/cart")
    // public String cart(HttpServletRequest request, Model model) {
    //     String subdomain = subdomainResolver.extractSubdomain(request);
    //     model.addAttribute("storeName", subdomain);
    //     return "store/cart";
    // }
}