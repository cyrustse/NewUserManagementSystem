package com.ums.service;

import com.ums.dto.user.CreateUserRequest;
import com.ums.dto.user.UpdateUserRequest;
import com.ums.dto.user.UserResponse;
import com.ums.dto.common.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    PageResponse<UserResponse> getUsers(int page, int size, String status, String search);

    UserResponse getUserById(UUID id);

    Optional<UserResponse> getUserById(String id);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void softDeleteUser(UUID id);

    void restoreUser(UUID id);

    void assignRole(UUID userId, UUID roleId, String scope, String scopeType);

    void removeRole(UUID userId, UUID roleId);

    UserResponse activateUser(UUID userId);

    UserResponse deactivateUser(UUID userId);

    void enableMfa(String userId);

    void setMfaSecret(String userId, String secret);

    String getMfaSecret(String userId);
}
