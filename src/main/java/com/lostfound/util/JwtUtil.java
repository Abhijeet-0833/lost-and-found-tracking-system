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

    private static String secretKeyString = "your_very_secret_key_that_should_be_long_and_secure";

    @Value("${jwt.secret:your_very_secret_key_that_should_be_long_and_secure}")
    public void setSecretKeyString(String secret) {
        if (secret != null && !secret.isBlank()) {
            secretKeyString = secret;
        }
    }

    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes());
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
