package com.swachhdrishti.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs on every /api/** request. Requires "Authorization: Bearer &lt;token&gt;"
 * and, when valid, exposes userId / email / role as request attributes
 * (read via {@link RequestUser}). Role checks live in the controllers.
 *
 * Deliberately NOT filtered:
 *  - anything outside /api/** (the Staff Portal's static files, /actuator/health)
 *  - CORS preflight (OPTIONS) requests, which browsers send WITHOUT a token
 *  - the login and public-config endpoints
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (!path.startsWith("/api/")) return true;
        return path.equals("/api/auth/login") || path.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            reject(response, "Missing or malformed Authorization header. Expected: Bearer <token>");
            return;
        }

        try {
            Claims claims = jwtService.parseToken(authHeader.substring(7));
            Number userId = (Number) claims.get("userId");
            request.setAttribute("userId", userId.longValue());
            request.setAttribute("email", claims.getSubject());
            request.setAttribute("role", claims.get("role", String.class));
        } catch (Exception e) {
            reject(response, "Invalid or expired token. Please log in again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
    }
}
