package com.ums.repository.impl;

import com.ums.entity.Permission;
import com.ums.entity.Resource;
import com.ums.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Permission> permissionRowMapper = (rs, rowNum) -> {
        Permission permission = new Permission();
        permission.setId(UUID.fromString(rs.getString("id")));
        permission.setName(rs.getString("name"));
        permission.setAction(rs.getString("action"));
        permission.setConditions(rs.getString("conditions"));

        // Map resource if present
        String resourceIdStr = rs.getString("resource_id");
        if (resourceIdStr != null) {
            Resource resource = new Resource();
            resource.setId(UUID.fromString(resourceIdStr));
            permission.setResource(resource);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            permission.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            permission.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            permission.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return permission;
    };

    @Override
    public Optional<Permission> findById(UUID id) {
        String sql = "SELECT p.id::text, p.name, p.resource_id::text, p.action, p.conditions, " +
                    "p.created_at, p.updated_at, p.deleted_at, r.id as resource_id " +
                    "FROM \"permissions\" p " +
                    "LEFT JOIN \"resources\" r ON p.resource_id = r.id " +
                    "WHERE p.id = ? AND p.deleted_at IS NULL";
        List<Permission> permissions = jdbcTemplate.query(sql, permissionRowMapper, id);
        return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
    }

    @Override
    public Optional<Permission> findByName(String name) {
        String sql = "SELECT p.id::text, p.name, p.resource_id::text, p.action, p.conditions, " +
                    "p.created_at, p.updated_at, p.deleted_at, r.id as resource_id " +
                    "FROM \"permissions\" p " +
                    "LEFT JOIN \"resources\" r ON p.resource_id = r.id " +
                    "WHERE p.name = ? AND p.deleted_at IS NULL";
        List<Permission> permissions = jdbcTemplate.query(sql, permissionRowMapper, name);
        return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
    }

    @Override
    public List<Permission> findAll(int page, int size) {
        String sql = "SELECT p.id::text, p.name, p.resource_id::text, p.action, p.conditions, " +
                    "p.created_at, p.updated_at, p.deleted_at, r.id as resource_id " +
                    "FROM \"permissions\" p " +
                    "LEFT JOIN \"resources\" r ON p.resource_id = r.id " +
                    "WHERE p.deleted_at IS NULL ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, permissionRowMapper, size, page * size);
    }

    @Override
    public List<Permission> findByResourceId(UUID resourceId) {
        String sql = "SELECT p.id::text, p.name, p.resource_id::text, p.action, p.conditions, " +
                    "p.created_at, p.updated_at, p.deleted_at, r.id as resource_id " +
                    "FROM \"permissions\" p " +
                    "LEFT JOIN \"resources\" r ON p.resource_id = r.id " +
                    "WHERE p.resource_id = ? AND p.deleted_at IS NULL";
        return jdbcTemplate.query(sql, permissionRowMapper, resourceId);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM \"permissions\" WHERE deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Override
    public Permission save(Permission permission) {
        if (permission.getId() == null) {
            String sql = "INSERT INTO \"permissions\" (id, name, resource_id, action, conditions, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            UUID id = UUID.randomUUID();
            permission.setId(id);
            jdbcTemplate.update(sql,
                id,
                permission.getName(),
                permission.getResource() != null ? permission.getResource().getId() : null,
                permission.getAction(),
                permission.getConditions(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        } else {
            String sql = "UPDATE \"permissions\" SET name = ?, resource_id = ?, action = ?, conditions = ?, " +
                        "updated_at = ?, deleted_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                permission.getName(),
                permission.getResource() != null ? permission.getResource().getId() : null,
                permission.getAction(),
                permission.getConditions(),
                Timestamp.valueOf(LocalDateTime.now()),
                permission.getDeletedAt() != null ? Timestamp.valueOf(permission.getDeletedAt()) : null,
                permission.getId()
            );
        }
        return permission;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "UPDATE \"permissions\" SET deleted_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM \"permissions\" WHERE name = ? AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class, name) > 0;
    }

    @Override
    public List<Permission> findAllActive() {
        String sql = "SELECT p.id::text, p.name, p.resource_id::text, p.action, p.conditions, " +
                    "p.created_at, p.updated_at, p.deleted_at, r.id as resource_id " +
                    "FROM \"permissions\" p " +
                    "LEFT JOIN \"resources\" r ON p.resource_id = r.id " +
                    "WHERE p.deleted_at IS NULL ORDER BY p.created_at DESC";
        return jdbcTemplate.query(sql, permissionRowMapper);
    }
}
