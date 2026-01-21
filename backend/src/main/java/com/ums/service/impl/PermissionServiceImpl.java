package com.ums.service.impl;

import com.ums.dto.common.PageResponse;
import com.ums.dto.permission.PermissionResponse;
import com.ums.entity.Permission;
import com.ums.entity.Resource;
import com.ums.repository.PermissionRepository;
import com.ums.repository.ResourceRepository;
import com.ums.service.AuditService;
import com.ums.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public PermissionResponse createPermission(String name, UUID resourceId, String action, String conditions) {
        if (permissionRepository.existsByName(name)) {
            throw new RuntimeException("Permission with name '" + name + "' already exists");
        }

        Permission permission = new Permission();
        permission.setName(name);
        permission.setAction(action);
        permission.setConditions(conditions);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());

        if (resourceId != null) {
            Resource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));
            permission.setResource(resource);
        }

        permission = permissionRepository.save(permission);

        auditService.logAuditEvent(
                com.ums.dto.audit.AuditEvent.builder()
                        .action("CREATE")
                        .entityType("Permission")
                        .entityId(permission.getId())
                        .newValue("{\"name\": \"" + name + "\", \"action\": \"" + action + "\"}")
                        .build()
        );

        return mapToResponse(permission);
    }

    @Override
    public PageResponse<PermissionResponse> getPermissions(int page, int size, String search) {
        List<Permission> permissions = permissionRepository.findAll(page, size);
        long total = permissionRepository.count();
        List<PermissionResponse> responses = permissions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(responses, page, size, total);
    }

    @Override
    public PermissionResponse getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        return mapToResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UUID id, String name, String action, String conditions) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        String oldValue = "{\"name\": \"" + permission.getName() + "\", \"action\": \"" + permission.getAction() + "\"}";

        if (name != null) {
            permission.setName(name);
        }
        if (action != null) {
            permission.setAction(action);
        }
        if (conditions != null) {
            permission.setConditions(conditions);
        }
        permission.setUpdatedAt(LocalDateTime.now());

        permission = permissionRepository.save(permission);

        String newValue = "{\"name\": \"" + permission.getName() + "\", \"action\": \"" + permission.getAction() + "\"}";

        auditService.logAuditEvent(
                com.ums.dto.audit.AuditEvent.builder()
                        .action("UPDATE")
                        .entityType("Permission")
                        .entityId(permission.getId())
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .build()
        );

        return mapToResponse(permission);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        permissionRepository.deleteById(id);

        auditService.logAuditEvent(
                com.ums.dto.audit.AuditEvent.builder()
                        .action("DELETE")
                        .entityType("Permission")
                        .entityId(id)
                        .oldValue("{\"name\": \"" + permission.getName() + "\"}")
                        .build()
        );
    }

    @Override
    public List<PermissionResponse> getPermissionsByResource(UUID resourceId) {
        List<Permission> permissions = permissionRepository.findByResourceId(resourceId);
        return permissions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PermissionResponse mapToResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setAction(permission.getAction());
        response.setConditions(permission.getConditions());
        response.setCreatedAt(permission.getCreatedAt());

        if (permission.getResource() != null) {
            response.setResource(permission.getResource().getName());
        }

        return response;
    }
}
