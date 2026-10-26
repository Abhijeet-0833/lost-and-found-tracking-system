package com.lostfound.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static String secretKeyString;

    @Value("${jwt.secret:}")
    public void setSecretKeyString(String secret) {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 bytes of random secret material.");
        }
        secretKeyString = secret;
    }

    private static Key getSigningKey() {
        if (secretKeyString == null || secretKeyString.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET is not configured securely.");
        }
        return Keys.hmacShaKeyFor(secretKeyString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
