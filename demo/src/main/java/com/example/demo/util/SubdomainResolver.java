package com.example.demo.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SubdomainResolver {

    @Value("${app.subdomain.main-host:localhost}")
    private String mainHost;

    /**
     * Priorité 1 : paramètre URL  ?store=madjid
     * Priorité 2 : sous-domaine   madjid.localhost:8080
     */
    public String extractSubdomain(HttpServletRequest request) {

        // ── Priorité 1 : ?store=madjid (pour ngrok) ──────────
        String storeParam = request.getParameter("store");
        if (storeParam != null && !storeParam.isBlank()) {
            return storeParam.toLowerCase().trim();
        }

        // ── Priorité 2 : sous-domaine classique ───────────────
        String host = request.getServerName();

        if (host == null || host.isEmpty()) return null;

        // Ignorer les adresses IP
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return null;

        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        if (host.equalsIgnoreCase(mainHost)) return null;

        String suffix = "." + mainHost;
        if (host.toLowerCase().endsWith(suffix)) {
            String subdomain = host.substring(
                0, host.length() - suffix.length());
            return subdomain.isEmpty() ? null : subdomain.toLowerCase();
        }

        return null;
    }
}