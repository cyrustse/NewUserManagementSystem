package com.ums.controller;

import com.ums.dto.common.ApiResponse;
import com.ums.dto.common.PageResponse;
import com.ums.dto.permission.PermissionResponse;
import com.ums.dto.role.*;
import com.ums.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/roles")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        
        try {
            RoleResponse response = roleService.createRole(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Role created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> getRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageResponse<RoleResponse> response = roleService.getRoles(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable String id) {
        
        try {
            UUID roleId = UUID.fromString(id);
            RoleResponse response = roleService.getRoleById(roleId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid role ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        try {
            UUID roleId = UUID.fromString(id);
            RoleResponse response = roleService.updateRole(roleId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid role ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String id) {
        
        try {
            UUID roleId = UUID.fromString(id);
            roleService.deleteRole(roleId);
            return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid role ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(@PathVariable String id) {
        
        try {
            UUID roleId = UUID.fromString(id);
            List<PermissionResponse> response = roleService.getRolePermissions(roleId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid role ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String id,
            @RequestBody List<UUID> permissionIds) {
        
        try {
            UUID roleId = UUID.fromString(id);
            roleService.updateRolePermissions(roleId, permissionIds);
            return ResponseEntity.ok(ApiResponse.success(null, "Role permissions updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid role ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
