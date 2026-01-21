package com.ums.controller;

import com.ums.dto.common.ApiResponse;
import com.ums.dto.common.PageResponse;
import com.ums.dto.permission.PermissionResponse;
import com.ums.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        try {
            PermissionResponse response = permissionService.createPermission(
                    request.getName(),
                    request.getResourceId(),
                    request.getAction(),
                    request.getConditions()
            );
            return ResponseEntity.ok(ApiResponse.success(response, "Permission created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<ApiResponse<PageResponse<PermissionResponse>>> getPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageResponse<PermissionResponse> response = permissionService.getPermissions(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(@PathVariable String id) {
        try {
            UUID permissionId = UUID.fromString(id);
            PermissionResponse response = permissionService.getPermissionById(permissionId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid permission ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable String id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        try {
            UUID permissionId = UUID.fromString(id);
            PermissionResponse response = permissionService.updatePermission(
                    permissionId,
                    request.getName(),
                    request.getAction(),
                    request.getConditions()
            );
            return ResponseEntity.ok(ApiResponse.success(response, "Permission updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid permission ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable String id) {
        try {
            UUID permissionId = UUID.fromString(id);
            permissionService.deletePermission(permissionId);
            return ResponseEntity.ok(ApiResponse.success(null, "Permission deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid permission ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByResource(
            @PathVariable String resourceId) {
        try {
            UUID uuid = UUID.fromString(resourceId);
            List<PermissionResponse> response = permissionService.getPermissionsByResource(uuid);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid resource ID format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    public static class CreatePermissionRequest {
        private String name;
        private UUID resourceId;
        private String action;
        private String conditions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public UUID getResourceId() {
            return resourceId;
        }

        public void setResourceId(UUID resourceId) {
            this.resourceId = resourceId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getConditions() {
            return conditions;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }
    }

    public static class UpdatePermissionRequest {
        private String name;
        private String action;
        private String conditions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getConditions() {
            return conditions;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }
    }
}
