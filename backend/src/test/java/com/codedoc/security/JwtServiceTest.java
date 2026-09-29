package com.codedoc.security;

import com.codedoc.user.Role;
import com.codedoc.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private org.springframework.security.core.userdetails.User userDetails;
    private final String secret = "my-ultra-secure-secret-key-that-is-at-least-32-bytes-long";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expiration);
        jwtService.init();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        userDetails = new org.springframework.security.core.userdetails.User(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertEquals("testuser", jwtService.extractUsername(token));
    }

    @Test
    void shouldHandleShortSecret() throws NoSuchAlgorithmException {
        JwtService shortSecretService = new JwtService();
        ReflectionTestUtils.setField(shortSecretService, "secretKey", "short");
        ReflectionTestUtils.setField(shortSecretService, "jwtExpiration", expiration);
        
        // This should trigger the warning log and SHA-256 hashing
        shortSecretService.init();
        
        String token = shortSecretService.generateToken(testUser);
        assertNotNull(token);
        assertEquals("testuser", shortSecretService.extractUsername(token));
    }

    @Test
    void shouldThrowExceptionForExpiredToken() {
        // Create an expired token manually
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    void shouldThrowExceptionForMalformedToken() {
        String malformedToken = "not.a.valid.token";
        assertThrows(MalformedJwtException.class, () -> jwtService.extractUsername(malformedToken));
    }

    @Test
    void shouldFailValidationForWrongUser() {
        String token = jwtService.generateToken(testUser);
        
        org.springframework.security.core.userdetails.User wrongUser = new org.springframework.security.core.userdetails.User(
                "wronguser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        assertFalse(jwtService.isTokenValid(token, wrongUser));
    }
}
