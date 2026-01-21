package com.ums.service.impl;

import com.ums.dto.user.CreateUserRequest;
import com.ums.dto.user.UpdateUserRequest;
import com.ums.dto.user.UserResponse;
import com.ums.dto.common.PageResponse;
import com.ums.entity.User;
import com.ums.entity.UserRole;
import com.ums.repository.*;
import com.ums.service.AuditService;
import com.ums.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements com.ums.service.UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private PasswordService passwordService;
    
    @Override
    public UserResponse createUser(CreateUserRequest request) {
        // Check for duplicates
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordService.hashPassword(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus(User.UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        // Log audit event (fire-and-forget)
        final UUID finalUserId = user.getId();
        final String finalUsername = user.getUsername();
        final String finalEmail = user.getEmail();
        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(null)
                .action("CREATE")
                .entityType("User")
                .entityId(finalUserId)
                .newValue("{\"username\": \"" + finalUsername + "\", \"email\": \"" + finalEmail + "\"}")
                .build()
        );

        return mapToResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getUsers(int page, int size, String status, String search) {
        List<User> users = userRepository.findAll(page, size, status, search);
        long total = userRepository.count(status, search);
        List<UserResponse> responses = users.stream().map(this::mapToResponse).collect(Collectors.toList());
        return new PageResponse<>(responses, page, size, total);
    }
    
    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }
    
    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String oldValue = "{\"username\": \"" + user.getUsername() + "\", \"email\": \"" + user.getEmail() + "\"}";
        
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(request.getStatus()));
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordService.hashPassword(request.getPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        
        String newValue = "{\"username\": \"" + user.getUsername() + "\", \"email\": \"" + user.getEmail() + "\"}";
        
        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(null)
                .action("UPDATE")
                .entityType("User")
                .entityId(user.getId())
                .oldValue(oldValue)
                .newValue(newValue)
                .build()
        );
        
        return mapToResponse(user);
    }
    
    @Override
    @Transactional
    public void softDeleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(null)
                .action("DELETE")
                .entityType("User")
                .entityId(user.getId())
                .build()
        );
    }
    
    @Override
    @Transactional
    public void restoreUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDeletedAt(null);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void assignRole(UUID userId, UUID roleId, String scope, String scopeType) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setScope(scope);
        userRole.setScopeType(scopeType);
        userRoleRepository.save(userRole);
    }
    
    @Override
    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String oldStatus = user.getStatus().name();
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(null)
                .action("ACTIVATE")
                .entityType("User")
                .entityId(user.getId())
                .oldValue("{\"status\": \"" + oldStatus + "\"}")
                .newValue("{\"status\": \"ACTIVE\"}")
                .build()
        );

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String oldStatus = user.getStatus().name();
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        auditService.logAuditEvent(
            com.ums.dto.audit.AuditEvent.builder()
                .userId(null)
                .action("DEACTIVATE")
                .entityType("User")
                .entityId(user.getId())
                .oldValue("{\"status\": \"" + oldStatus + "\"}")
                .newValue("{\"status\": \"INACTIVE\"}")
                .build()
        );

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void enableMfa(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMfaEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setMfaSecret(String userId, String secret) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMfaSecret(secret);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public String getMfaSecret(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getMfaSecret();
    }

    @Override
    public Optional<UserResponse> getUserById(String id) {
        try {
            return Optional.of(getUserById(UUID.fromString(id)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus().name());
        response.setMfaEnabled(user.isMfaEnabled());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        
        // Get user roles
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<String> roles = userRoles.stream()
            .map(ur -> ur.getRoleId().toString())
            .collect(Collectors.toList());
        response.setRoles(roles);
        
        return response;
    }
}
