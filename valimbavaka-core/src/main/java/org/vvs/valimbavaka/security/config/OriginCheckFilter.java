package org.vvs.valimbavaka.security.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Le refresh token voyage dans un cookie : CSRF désactivé, on refuse donc toute requête de rotation ou
 * de déconnexion dont l'en-tête {@code Origin} n'est pas une origine autorisée (convention §9.4).
 */
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    private final Set<String> allowedOrigins;
    private final AccessDeniedHandler accessDeniedHandler;

    public OriginCheckFilter(Set<String> allowedOrigins, AccessDeniedHandler accessDeniedHandler) {
        this.allowedOrigins = allowedOrigins;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !allowedOrigins.contains(origin)) {
            accessDeniedHandler.handle(request, response, new AccessDeniedException("Origine non autorisée"));
            return;
        }
        chain.doFilter(request, response);
    }
}
