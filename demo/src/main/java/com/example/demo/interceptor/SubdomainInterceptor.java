package com.example.demo.interceptor;

import com.example.demo.util.SubdomainResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SubdomainInterceptor implements HandlerInterceptor {

    private final SubdomainResolver subdomainResolver;

    public SubdomainInterceptor(SubdomainResolver subdomainResolver) {
        this.subdomainResolver = subdomainResolver;
    }

    // All paths that must NEVER trigger a redirect
    private static final String[] EXCLUDED_PREFIXES = {
        "/store",
        "/images",
        "/css",
        "/js",
        "/login",
        "/logout",
        "/error",
        "/favicon.ico",
        "/register",
        "/admin",
        "/user",
        "/products"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {

        String subdomain = subdomainResolver.extractSubdomain(request);
        String uri       = request.getRequestURI();

        // Pas de subdomain → app normale
        if (subdomain == null) return true;

        // Déjà sur une page store ou ressource statique → continuer
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }

        // Redirect avec le subdomain extrait du host
        String redirectUrl = request.getScheme()
                + "://" + request.getServerName()
                + ":" + request.getServerPort()
                + "/store/home";

        response.sendRedirect(redirectUrl);
        return false;
    }
}