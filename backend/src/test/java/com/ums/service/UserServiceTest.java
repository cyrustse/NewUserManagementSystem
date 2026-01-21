package com.ums.service;

import com.ums.dto.user.CreateUserRequest;
import com.ums.dto.user.UpdateUserRequest;
import com.ums.dto.user.UserResponse;
import com.ums.dto.common.PageResponse;
import com.ums.entity.User;
import com.ums.repository.UserRepository;
import com.ums.repository.UserRoleRepository;
import com.ums.service.AuditService;
import com.ums.util.PasswordService;
import com.ums.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private AuditService auditService;
    private PasswordService passwordService;
    private UserServiceImpl userService;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        passwordService = new PasswordService();
        userService = new UserServiceImpl();
        setField(userService, "userRepository", userRepository);
        setField(userService, "userRoleRepository", userRoleRepository);
        setField(userService, "auditService", auditService);
        setField(userService, "passwordService", passwordService);
    }

    @Test
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setPhone("1234567890");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getUserById_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        UserResponse response = userService.getUserById(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void softDeleteUser_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setStatus(User.UserStatus.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> userService.softDeleteUser(userId));

        verify(userRepository).save(argThat(u -> u.getDeletedAt() != null && u.getStatus() == User.UserStatus.INACTIVE));
    }
}
