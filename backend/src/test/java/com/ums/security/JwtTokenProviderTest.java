package com.ums.security;

import com.ums.entity.Role;
import com.ums.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
        jwtTokenProvider = new JwtTokenProvider(secret, 1800, 604800, "test-issuer");
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }
    
    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        // Given
        Role role = new Role();
        role.setName("USER");
        role.setPriority(40);
        
        // When
        String token = jwtTokenProvider.generateAccessToken(testUser, List.of(role));
        
        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);  // JWT has 3 parts
    }
    
    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateRefreshToken(testUser);
        
        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
    
    @Test
    void generateTempToken_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateTempToken(testUser);
        
        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
    
    @Test
    void validateToken_WithValidToken_ShouldReturnClaims() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, List.of());
        
        // When
        var claims = jwtTokenProvider.validateToken(token);
        
        // Then
        assertNotNull(claims);
        assertEquals(testUser.getId().toString(), claims.getSubject());
    }
    
    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When & Then
        assertThrows(Exception.class, () -> 
            jwtTokenProvider.validateToken(invalidToken));
    }
    
    @Test
    void isRefreshToken_WithRefreshToken_ShouldReturnTrue() {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);
        
        // When
        boolean isRefresh = jwtTokenProvider.isRefreshToken(refreshToken);
        
        // Then
        assertTrue(isRefresh);
    }
    
    @Test
    void isAccessToken_WithAccessToken_ShouldReturnTrue() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(testUser, List.of());
        
        // When
        boolean isAccess = jwtTokenProvider.isAccessToken(accessToken);
        
        // Then
        assertTrue(isAccess);
    }
    
    @Test
    void getUserIdFromToken_ShouldReturnCorrectId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, List.of());
        
        // When
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertEquals(testUser.getId(), userId);
    }
    
    @Test
    void accessToken_ShouldNotBeRefreshToken() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(testUser, List.of());
        
        // When
        boolean isRefresh = jwtTokenProvider.isRefreshToken(accessToken);
        
        // Then
        assertFalse(isRefresh);
    }
}
