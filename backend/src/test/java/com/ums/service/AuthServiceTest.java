package com.ums.service;

import com.ums.dto.auth.LoginRequest;
import com.ums.dto.auth.LoginResponse;
import com.ums.entity.Role;
import com.ums.entity.User;
import com.ums.repository.RefreshTokenRepository;
import com.ums.repository.RoleRepository;
import com.ums.repository.UserRepository;
import com.ums.repository.UserRoleRepository;
import com.ums.security.JwtTokenProvider;
import com.ums.service.AuditService;
import com.ums.service.CacheService;
import com.ums.service.OpaService;
import com.ums.service.RateLimitService;
import com.ums.util.PasswordService;
import com.ums.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuditService auditService;
    @Mock
    private CacheService cacheService;
    @Mock
    private OpaService opaService;
    @Mock
    private RateLimitService rateLimitService;
    private PasswordService passwordService;
    private AuthServiceImpl authService;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        passwordService = new PasswordService();
        authService = new AuthServiceImpl();
        setField(authService, "userRepository", userRepository);
        setField(authService, "refreshTokenRepository", refreshTokenRepository);
        setField(authService, "userRoleRepository", userRoleRepository);
        setField(authService, "roleRepository", roleRepository);
        setField(authService, "jwtTokenProvider", jwtTokenProvider);
        setField(authService, "auditService", auditService);
        setField(authService, "cacheService", cacheService);
        setField(authService, "opaService", opaService);
        setField(authService, "rateLimitService", rateLimitService);
        setField(authService, "passwordService", passwordService);
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordService.hashPassword("password123"));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setMfaEnabled(false);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("USER");

        List<Role> roles = new ArrayList<>();
        roles.add(role);

        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(user.isLocked()).thenReturn(false);
        when(passwordService.verifyPassword("password123", user.getPasswordHash())).thenReturn(true);
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(new ArrayList<>());
        when(roleRepository.findById(any())).thenReturn(Optional.of(role));
        when(jwtTokenProvider.generateAccessToken(any(), anyList())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");

        LoginResponse response = authService.login(request.getUsernameOrEmail(), request.getPassword(), "127.0.0.1", "Mozilla/5.0");

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("wrongpassword");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordService.hashPassword("password123"));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);

        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(user.isLocked()).thenReturn(false);
        when(passwordService.verifyPassword("wrongpassword", user.getPasswordHash())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request.getUsernameOrEmail(), request.getPassword(), "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_RateLimited_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");

        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request.getUsernameOrEmail(), request.getPassword(), "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals("Too many login attempts. Please try again later.", exception.getMessage());
    }

    @Test
    void login_UserLocked_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setStatus(User.UserStatus.LOCKED);
        user.setLockedUntil(LocalDateTime.now().plusHours(1));

        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(user.isLocked()).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request.getUsernameOrEmail(), request.getPassword(), "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals("Account is temporarily locked. Please try again later.", exception.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request.getUsernameOrEmail(), request.getPassword(), "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void verifyMfa_ReturnsTrue() {
        boolean result = authService.verifyMfa("123456", "tempToken");
        assertTrue(result);
    }
}
