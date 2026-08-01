package com.smartbus.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(padSecret(jwtProperties.getSecret()).getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Long driverId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMinutes() * 60_000L);
        return Jwts.builder()
                .subject(username)
                .claim("driverId", driverId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getDriverId(String token) {
        Object value = parseClaims(token).get("driverId");
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public long getExpirationMinutes() {
        return jwtProperties.getExpirationMinutes();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static String padSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.secret must be set (env JWT_SECRET) and at least 32 bytes"
            );
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least 32 bytes; set JWT_SECRET in the environment"
            );
        }
        return secret;
    }
}
