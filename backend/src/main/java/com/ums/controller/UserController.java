package com.ums.controller;

import com.ums.dto.common.ApiResponse;
import com.ums.dto.common.PageResponse;
import com.ums.dto.user.*;
import com.ums.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UUID userId = UUID.fromString(userIdStr);
            UserResponse response = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        try {
            UserResponse response = userService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success(response, "User created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        PageResponse<UserResponse> response = userService.getUsers(page, size, status, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        
        try {
            UUID userId = UUID.fromString(id);
            UserResponse response = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        try {
            UUID userId = UUID.fromString(id);
            UserResponse response = userService.updateUser(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {

        try {
            UUID userId = UUID.fromString(id);
            userService.softDeleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable String id) {

        try {
            UUID userId = UUID.fromString(id);
            UserResponse response = userService.activateUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "User activated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable String id) {

        try {
            UUID userId = UUID.fromString(id);
            UserResponse response = userService.deactivateUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "User deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String id,
            @RequestBody RoleAssignmentRequest request) {
        
        try {
            UUID userId = UUID.fromString(id);
            userService.assignRole(userId, request.getRoleId(), request.getScope(), request.getScopeType());
            return ResponseEntity.ok(ApiResponse.success(null, "Role assigned successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String id,
            @PathVariable String roleId) {
        
        try {
            UUID userId = UUID.fromString(id);
            UUID roleUuid = UUID.fromString(roleId);
            userService.removeRole(userId, roleUuid);
            return ResponseEntity.ok(ApiResponse.success(null, "Role removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<Void>> restoreUser(@PathVariable String id) {
        
        try {
            UUID userId = UUID.fromString(id);
            userService.restoreUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null, "User restored successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid user ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
