package com.smartbus.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-at-least-32-characters-long");
        properties.setExpirationMinutes(60L);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtTokenProvider.generateToken("driver1", 10L);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("driver1", jwtTokenProvider.getSubject(token));
        assertEquals(10L, jwtTokenProvider.getDriverId(token));
    }
}
