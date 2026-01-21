package com.ums.service.impl;

import com.ums.dto.permission.PermissionResponse;
import com.ums.dto.role.CreateRoleRequest;
import com.ums.dto.role.UpdateRoleRequest;
import com.ums.dto.role.RoleResponse;
import com.ums.dto.common.PageResponse;
import com.ums.entity.Permission;
import com.ums.entity.Role;
import com.ums.entity.RolePermission;
import com.ums.repository.*;
import com.ums.service.OpaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements com.ums.service.RoleService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private OpaService opaService;
    
    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("Role name already exists");
        }
        
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setPriority(request.getPriority());
        role.setSystem(false);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        
        if (request.getParentId() != null) {
            roleRepository.findById(UUID.fromString(request.getParentId()))
                .ifPresent(role::setParent);
        }
        
        role = roleRepository.save(role);
        opaService.refreshOpaData();
        return mapToResponse(role);
    }
    
    @Override
    public PageResponse<RoleResponse> getRoles(int page, int size) {
        List<Role> roles = roleRepository.findAll(page, size);
        long total = roleRepository.count();
        List<RoleResponse> responses = roles.stream().map(this::mapToResponse).collect(Collectors.toList());
        return new PageResponse<>(responses, page, size, total);
    }
    
    @Override
    public RoleResponse getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
        return mapToResponse(role);
    }
    
    @Override
    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (role.isSystem()) {
            throw new RuntimeException("Cannot modify system roles");
        }
        
        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            role.setPriority(request.getPriority());
        }
        if (request.getParentId() != null) {
            roleRepository.findById(UUID.fromString(request.getParentId()))
                .ifPresent(role::setParent);
        }
        
        role.setUpdatedAt(LocalDateTime.now());
        role = roleRepository.save(role);
        opaService.refreshOpaData();
        
        return mapToResponse(role);
    }
    
    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (role.isSystem()) {
            throw new RuntimeException("Cannot delete system roles");
        }
        
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
        opaService.refreshOpaData();
    }
    
    @Override
    @Transactional
    public void assignPermission(UUID roleId, UUID permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermissionRepository.save(rolePermission);
        opaService.refreshOpaData();
    }
    
    @Override
    @Transactional
    public void revokePermission(UUID roleId, UUID permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        opaService.refreshOpaData();
    }
    
    @Override
    public List<PermissionResponse> getRolePermissions(UUID roleId) {
        if (!roleRepository.findById(roleId).isPresent()) {
            throw new RuntimeException("Role not found");
        }
        
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        
        return rolePermissions.stream()
            .map(rp -> {
                Permission permission = permissionRepository.findById(rp.getPermissionId())
                    .orElseThrow(() -> new RuntimeException("Permission not found"));
                return mapToPermissionResponse(permission);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void updateRolePermissions(UUID roleId, List<UUID> permissionIds) {
        if (!roleRepository.findById(roleId).isPresent()) {
            throw new RuntimeException("Role not found");
        }
        
        rolePermissionRepository.deleteByRoleId(roleId);
        
        for (UUID permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionRepository.save(rolePermission);
        }
        
        opaService.refreshOpaData();
    }
    
    private RoleResponse mapToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setSystem(role.isSystem());
        response.setPriority(role.getPriority());
        response.setCreatedAt(role.getCreatedAt());

        // Add user count
        int userCount = userRoleRepository.countByRoleId(role.getId());
        response.setUserCount(userCount);

        if (role.getParent() != null) {
            response.setParentId(role.getParent().getId().toString());
            response.setParentName(role.getParent().getName());
        }

        return response;
    }
    
    private PermissionResponse mapToPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setAction(permission.getAction());
        response.setConditions(permission.getConditions());
        response.setCreatedAt(permission.getCreatedAt());
        
        if (permission.getResource() != null) {
            response.setResource(permission.getResource().getId().toString());
        }
        
        return response;
    }
}
