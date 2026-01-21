package com.ums.repository.impl;

import com.ums.entity.RolePermission;
import com.ums.repository.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class RolePermissionRepositoryImpl implements RolePermissionRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<RolePermission> rolePermissionRowMapper = (rs, rowNum) -> {
        RolePermission rp = new RolePermission();
        rp.setId(UUID.fromString(rs.getString("id")));
        rp.setRoleId(UUID.fromString(rs.getString("role_id")));
        rp.setPermissionId(UUID.fromString(rs.getString("permission_id")));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            rp.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return rp;
    };
    
    @Override
    public RolePermission save(RolePermission rolePermission) {
        if (rolePermission.getId() == null) {
            String sql = "INSERT INTO \"role_permissions\" (id, role_id, permission_id, created_at) " +
                        "VALUES (?, ?, ?, ?)";
            UUID id = UUID.randomUUID();
            rolePermission.setId(id);
            jdbcTemplate.update(sql,
                id,
                rolePermission.getRoleId(),
                rolePermission.getPermissionId(),
                LocalDateTime.now()
            );
        } else {
            String sql = "UPDATE \"role_permissions\" SET role_id = ?, permission_id = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                rolePermission.getRoleId(),
                rolePermission.getPermissionId(),
                rolePermission.getId()
            );
        }
        return rolePermission;
    }
    
    @Override
    public List<RolePermission> findByRoleId(UUID roleId) {
        String sql = "SELECT * FROM \"role_permissions\" WHERE role_id = ?";
        return jdbcTemplate.query(sql, rolePermissionRowMapper, roleId);
    }
    
    @Override
    public List<RolePermission> findByPermissionId(UUID permissionId) {
        String sql = "SELECT * FROM \"role_permissions\" WHERE permission_id = ?";
        return jdbcTemplate.query(sql, rolePermissionRowMapper, permissionId);
    }
    
    @Override
    public void deleteByRoleId(UUID roleId) {
        String sql = "DELETE FROM \"role_permissions\" WHERE role_id = ?";
        jdbcTemplate.update(sql, roleId);
    }
    
    @Override
    public void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        String sql = "DELETE FROM \"role_permissions\" WHERE role_id = ? AND permission_id = ?";
        jdbcTemplate.update(sql, roleId, permissionId);
    }
    
    @Override
    public List<RolePermission> findAll() {
        String sql = "SELECT * FROM \"role_permissions\"";
        return jdbcTemplate.query(sql, rolePermissionRowMapper);
    }
}
