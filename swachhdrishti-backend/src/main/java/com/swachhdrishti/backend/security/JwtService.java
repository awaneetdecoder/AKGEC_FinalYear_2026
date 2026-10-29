package com.swachhdrishti.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and checks the login tokens (JWTs) that prove who is making a
 * request. A JWT is a signed, tamper-proof piece of text carrying the user's
 * id, e-mail and role. The same token format is accepted from the Flutter
 * app and from the browser-based Staff Portal.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-hours:24}")
    private long expiryHours;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters long (set the JWT_SECRET environment variable).");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryHours * 60L * 60L * 1000L);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Throws if the token is malformed, tampered with, or expired. */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
