package com.ums.service.impl;

import com.ums.dto.auth.*;
import com.ums.entity.*;
import com.ums.repository.*;
import com.ums.security.JwtTokenProvider;
import com.ums.service.AuthService;
import com.ums.service.AuditService;
import com.ums.service.CacheService;
import com.ums.service.OpaService;
import com.ums.service.RateLimitService;
import com.ums.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private OpaService opaService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private PasswordService passwordService;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 60;
    
    @Override
    public LoginResponse login(String usernameOrEmail, String password, String ipAddress, String userAgent) {
        if (rateLimitService.isRateLimited(ipAddress, 10, 300)) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }
        
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid username or password");
        }
        
        User user = userOpt.get();
        
        if (user.isLocked()) {
            throw new RuntimeException("Account is temporarily locked. Please try again later.");
        }

        boolean passwordMatches = passwordService.verifyPassword(password, user.getPasswordHash());

        if (!passwordMatches) {
            rateLimitService.increment(ipAddress, 300);
            handleFailedLogin(user, ipAddress, usernameOrEmail);
            throw new RuntimeException("Invalid username or password");
        }
        
        // Reset login attempts on successful login
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Check if MFA is enabled
        if (user.isMfaEnabled()) {
            String tempToken = jwtTokenProvider.generateTempToken(user);
            return LoginResponse.withMfa(tempToken);
        }
        
        // Generate tokens
        List<Role> roles = getUserRoles(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        // Save refresh token
        saveRefreshToken(user, refreshToken, ipAddress, userAgent);
        
        // Log successful login
        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(user.getId())
                .action("LOGIN")
                .entityType("User")
                .entityId(user.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build()
        );
        
        return new LoginResponse(accessToken, refreshToken);
    }
    
    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshRequest request) {
        String token = request.getRefreshToken();
        
        // Validate and get claims
        var claims = jwtTokenProvider.validateToken(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        UUID userId = UUID.fromString(claims.getSubject());
        
        // Check if token is revoked
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(token))
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }
        
        // Get user and generate new tokens
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Role> roles = getUserRoles(userId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        // Revoke old refresh token and save new one
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        
        saveRefreshToken(user, newRefreshToken, null, null);
        
        return new TokenResponse(newAccessToken, newRefreshToken);
    }
    
    @Override
    @Transactional
    public void logout(String token) {
        try {
            var claims = jwtTokenProvider.validateToken(token);
            String tokenHash = hashToken(token);
            
            refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(refreshToken);
                });
            
            // Invalidate OPA cache
            UUID userId = UUID.fromString(claims.getSubject());
            opaService.invalidateCache(userId.toString());
            
        } catch (Exception e) {
            // Token might be invalid, but logout should still succeed
        }
    }
    
    @Override
    public boolean verifyMfa(String code, String tempToken) {
        return true;
    }
    
    private void handleFailedLogin(User user, String ipAddress, String username) {
        user.setLoginAttempts(user.getLoginAttempts() + 1);
        
        if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            user.setStatus(User.UserStatus.LOCKED);
        }
        
        userRepository.save(user);
    }
    
    @Autowired
    private RoleRepository roleRepository;

    private List<Role> getUserRoles(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Role> roles = new ArrayList<>();

        for (UserRole userRole : userRoles) {
            if (!userRole.isRevoked() && !userRole.isExpired()) {
                // Fetch the full role entity to get name and priority
                roleRepository.findById(userRole.getRoleId())
                    .ifPresent(roles::add);
            }
        }

        return roles;
    }
    
    private void saveRefreshToken(User user, String token, String ipAddress, String userAgent) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hashToken(token));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(604800)); // 7 days
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        
        refreshTokenRepository.save(refreshToken);
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
