package com.ums.repository;

import com.ums.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    
    Optional<User> findById(UUID id);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    List<User> findAll(int page, int size, String status, String search);
    
    long count(String status, String search);
    
    User save(User user);
    
    void deleteById(UUID id);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
}
