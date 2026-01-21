package com.ums.repository.impl;

import com.ums.entity.UserRole;
import com.ums.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleRepositoryImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<UserRole> userRoleRowMapper = (rs, rowNum) -> {
        UserRole userRole = new UserRole();
        userRole.setId(UUID.fromString(rs.getString("id")));
        userRole.setUserId(UUID.fromString(rs.getString("user_id")));
        userRole.setRoleId(UUID.fromString(rs.getString("role_id")));
        userRole.setScope(rs.getString("scope"));
        userRole.setScopeType(rs.getString("scope_type"));
        
        Timestamp grantedAt = rs.getTimestamp("granted_at");
        if (grantedAt != null) {
            userRole.setGrantedAt(grantedAt.toLocalDateTime());
        }
        
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            userRole.setExpiresAt(expiresAt.toLocalDateTime());
        }
        
        String grantedBy = rs.getString("granted_by");
        if (grantedBy != null) {
            userRole.setGrantedBy(UUID.fromString(grantedBy));
        }
        
        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        if (revokedAt != null) {
            userRole.setRevokedAt(revokedAt.toLocalDateTime());
        }
        
        return userRole;
    };
    
    @Override
    public UserRole save(UserRole userRole) {
        if (userRole.getId() == null) {
            UUID id = UUID.randomUUID();
            userRole.setId(id);

            String sql = "INSERT INTO \"user_roles\" (id, user_id, role_id, scope, scope_type, granted_at, expires_at, granted_by) " +
                        "VALUES (:id, :userId, :roleId, :scope, CAST(:scopeType AS scope_type), :grantedAt, :expiresAt, :grantedBy)";

            java.util.HashMap<String, Object> params = new java.util.HashMap<>();
            params.put("id", id);
            params.put("userId", userRole.getUserId());
            params.put("roleId", userRole.getRoleId());
            params.put("scope", userRole.getScope());
            params.put("scopeType", userRole.getScopeType());
            params.put("grantedAt", userRole.getGrantedAt() != null ? Timestamp.valueOf(userRole.getGrantedAt()) : null);
            params.put("expiresAt", userRole.getExpiresAt() != null ? Timestamp.valueOf(userRole.getExpiresAt()) : null);
            params.put("grantedBy", userRole.getGrantedBy());

            logger.info("DEBUG: Inserting user_role with id={}, userId={}, roleId={}, scope={}, scopeType={}, grantedAt={}, expiresAt={}, grantedBy={}",
                id, userRole.getUserId(), userRole.getRoleId(), userRole.getScope(), userRole.getScopeType(),
                userRole.getGrantedAt(), userRole.getExpiresAt(), userRole.getGrantedBy());
            logger.info("DEBUG_SQL: {}", sql);
            logger.info("DEBUG_PARAMS: {}", params);

            try {
                namedParameterJdbcTemplate.update(sql, params);
                logger.info("DEBUG: Insert successful");
            } catch (Exception e) {
                logger.error("DEBUG: Insert failed: " + e.getMessage(), e);
                throw e;
            }
        }
        return userRole;
    }
    
    @Override
    public List<UserRole> findByUserId(UUID userId) {
        String sql = "SELECT * FROM \"user_roles\" WHERE user_id = ? AND revoked_at IS NULL";
        return jdbcTemplate.query(sql, userRoleRowMapper, userId);
    }
    
    @Override
    public List<UserRole> findByRoleId(UUID roleId) {
        String sql = "SELECT * FROM \"user_roles\" WHERE role_id = ? AND revoked_at IS NULL";
        return jdbcTemplate.query(sql, userRoleRowMapper, roleId);
    }

    @Override
    public int countByRoleId(UUID roleId) {
        String sql = "SELECT COUNT(*) FROM \"user_roles\" WHERE role_id = ? AND revoked_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Integer.class, roleId);
    }

    @Override
    public void deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        String sql = "UPDATE \"user_roles\" SET revoked_at = ? WHERE user_id = ? AND role_id = ?";
        jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), userId, roleId);
    }
    
    @Override
    public void revokeByUserId(UUID userId) {
        String sql = "UPDATE \"user_roles\" SET revoked_at = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), userId);
    }
}
