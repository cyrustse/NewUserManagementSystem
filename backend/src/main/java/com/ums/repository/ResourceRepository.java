package com.ums.repository;

import com.ums.entity.Resource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository {
    Optional<Resource> findById(UUID id);
    Optional<Resource> findByName(String name);
    boolean existsByName(String name);
    List<Resource> findAll();
    Resource save(Resource resource);
    void deleteById(UUID id);
}
