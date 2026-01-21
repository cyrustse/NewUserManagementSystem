package com.ums.repository;

import com.ums.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
    
    Optional<Permission> findById(UUID id);
    
    Optional<Permission> findByName(String name);
    
    List<Permission> findAll(int page, int size);
    
    List<Permission> findByResourceId(UUID resourceId);
    
    long count();
    
    Permission save(Permission permission);
    
    void deleteById(UUID id);
    
    boolean existsByName(String name);
    
    List<Permission> findAllActive();
}
