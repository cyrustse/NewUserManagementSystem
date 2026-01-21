package com.ums.service;

import com.ums.dto.permission.PermissionResponse;
import com.ums.dto.role.CreateRoleRequest;
import com.ums.dto.role.UpdateRoleRequest;
import com.ums.dto.role.RoleResponse;
import com.ums.dto.common.PageResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    
    RoleResponse createRole(CreateRoleRequest request);
    
    PageResponse<RoleResponse> getRoles(int page, int size);
    
    RoleResponse getRoleById(UUID id);
    
    RoleResponse updateRole(UUID id, UpdateRoleRequest request);
    
    void deleteRole(UUID id);
    
    void assignPermission(UUID roleId, UUID permissionId);
    
    void revokePermission(UUID roleId, UUID permissionId);
    
    List<PermissionResponse> getRolePermissions(UUID roleId);
    
    void updateRolePermissions(UUID roleId, List<UUID> permissionIds);
}
