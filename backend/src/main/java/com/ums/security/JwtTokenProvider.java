package com.ums.security;

import com.ums.entity.Role;
import com.ums.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final String issuer;
    
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.issuer}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;
    }
    
    public String generateAccessToken(User user, List<Role> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);
        
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("roles", roles.stream().map(Role::getName).filter(Objects::nonNull).toList())
            .claim("role_priorities", roles.stream().mapToInt(Role::getPriority).max().orElse(0))
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
    
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);
        
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("type", "refresh")
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
    
    public String generateTempToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 5 * 60 * 1000); // 5 minutes
        
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("type", "mfa_temp")
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
    
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }
    
    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }
    
    public boolean isAccessToken(String token) {
        Claims claims = validateToken(token);
        return !"refresh".equals(claims.get("type", String.class)) && 
               !"mfa_temp".equals(claims.get("type", String.class));
    }
}
