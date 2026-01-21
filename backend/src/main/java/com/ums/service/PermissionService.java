package com.ums.service;

import com.ums.dto.common.PageResponse;
import com.ums.dto.permission.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface PermissionService {

    PermissionResponse createPermission(String name, UUID resourceId, String action, String conditions);

    PageResponse<PermissionResponse> getPermissions(int page, int size, String search);

    PermissionResponse getPermissionById(UUID id);

    PermissionResponse updatePermission(UUID id, String name, String action, String conditions);

    void deletePermission(UUID id);

    List<PermissionResponse> getPermissionsByResource(UUID resourceId);
}
