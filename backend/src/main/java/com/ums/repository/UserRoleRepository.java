package com.ums.repository;

import com.ums.entity.UserRole;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository {
    
    UserRole save(UserRole userRole);
    
    List<UserRole> findByUserId(UUID userId);
    
    List<UserRole> findByRoleId(UUID roleId);

    int countByRoleId(UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    
    void revokeByUserId(UUID userId);
}
