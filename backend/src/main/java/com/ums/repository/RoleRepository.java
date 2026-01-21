package com.ums.repository;

import com.ums.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    
    Optional<Role> findById(UUID id);
    
    Optional<Role> findByName(String name);
    
    List<Role> findAll(int page, int size);
    
    List<Role> findByParentId(UUID parentId);
    
    List<Role> findHierarchy(UUID roleId);
    
    long count();
    
    Role save(Role role);
    
    void deleteById(UUID id);
    
    boolean existsByName(String name);
    
    List<Role> findAllActive();
}
