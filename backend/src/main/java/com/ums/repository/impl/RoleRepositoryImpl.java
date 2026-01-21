package com.ums.repository.impl;

import com.ums.entity.Role;
import com.ums.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Role> roleRowMapper = (rs, rowNum) -> {
        Role role = new Role();
        role.setId(UUID.fromString(rs.getString("id")));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        role.setSystem(rs.getBoolean("is_system"));
        role.setPriority(rs.getInt("priority"));
        
        String parentId = rs.getString("parent_id");
        if (parentId != null) {
            Role parent = new Role();
            parent.setId(UUID.fromString(parentId));
            role.setParent(parent);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            role.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            role.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            role.setDeletedAt(deletedAt.toLocalDateTime());
        }
        
        return role;
    };
    
    @Override
    public Optional<Role> findById(UUID id) {
        String sql = "SELECT * FROM \"roles\" WHERE id = ? AND deleted_at IS NULL";
        List<Role> roles = jdbcTemplate.query(sql, roleRowMapper, id);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }
    
    @Override
    public Optional<Role> findByName(String name) {
        String sql = "SELECT * FROM \"roles\" WHERE name = ? AND deleted_at IS NULL";
        List<Role> roles = jdbcTemplate.query(sql, roleRowMapper, name);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }
    
    @Override
    public List<Role> findAll(int page, int size) {
        String sql = "SELECT * FROM \"roles\" WHERE deleted_at IS NULL ORDER BY priority DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, roleRowMapper, size, page * size);
    }
    
    @Override
    public List<Role> findByParentId(UUID parentId) {
        String sql = "SELECT * FROM \"roles\" WHERE parent_id = ? AND deleted_at IS NULL";
        return jdbcTemplate.query(sql, roleRowMapper, parentId);
    }
    
    @Override
    public List<Role> findHierarchy(UUID roleId) {
        // Simplified hierarchy lookup
        return findAll(0, 100);
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM \"roles\" WHERE deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    @Override
    public Role save(Role role) {
        if (role.getId() == null) {
            String sql = "INSERT INTO \"roles\" (id, name, description, is_system, parent_id, priority, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            UUID id = UUID.randomUUID();
            role.setId(id);
            jdbcTemplate.update(sql,
                id,
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                role.getParent() != null ? role.getParent().getId() : null,
                role.getPriority(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        } else {
            String sql = "UPDATE \"roles\" SET name = ?, description = ?, is_system = ?, parent_id = ?, priority = ?, " +
                        "updated_at = ?, deleted_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                role.getParent() != null ? role.getParent().getId() : null,
                role.getPriority(),
                Timestamp.valueOf(LocalDateTime.now()),
                role.getDeletedAt() != null ? Timestamp.valueOf(role.getDeletedAt()) : null,
                role.getId()
            );
        }
        return role;
    }
    
    @Override
    public void deleteById(UUID id) {
        String sql = "UPDATE \"roles\" SET deleted_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
    
    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM \"roles\" WHERE name = ? AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class, name) > 0;
    }
    
    @Override
    public List<Role> findAllActive() {
        String sql = "SELECT * FROM \"roles\" WHERE deleted_at IS NULL ORDER BY priority DESC";
        return jdbcTemplate.query(sql, roleRowMapper);
    }
}
