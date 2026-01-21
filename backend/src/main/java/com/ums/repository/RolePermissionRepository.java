package com.ums.repository;

import com.ums.entity.RolePermission;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository {
    
    RolePermission save(RolePermission rolePermission);
    
    List<RolePermission> findByRoleId(UUID roleId);
    
    List<RolePermission> findByPermissionId(UUID permissionId);
    
    void deleteByRoleId(UUID roleId);
    
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    
    List<RolePermission> findAll();
}
