package com.ums.repository.impl;

import com.ums.entity.AuditLog;
import com.ums.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<AuditLog> auditLogRowMapper = (rs, rowNum) -> {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.fromString(rs.getString("id")));
        
        String userId = rs.getString("user_id");
        if (userId != null) {
            auditLog.setUserId(UUID.fromString(userId));
        }
        
        auditLog.setAction(rs.getString("action"));
        auditLog.setEntityType(rs.getString("entity_type"));
        
        String entityId = rs.getString("entity_id");
        if (entityId != null) {
            auditLog.setEntityId(UUID.fromString(entityId));
        }
        
        auditLog.setOldValue(rs.getString("old_value"));
        auditLog.setNewValue(rs.getString("new_value"));
        auditLog.setIpAddress(rs.getString("ip_address"));
        auditLog.setUserAgent(rs.getString("user_agent"));
        auditLog.setMetadata(rs.getString("metadata"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            auditLog.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return auditLog;
    };
    
    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            // PostgreSQL ENUM type requires explicit casting
            String sql = "INSERT INTO \"audit_logs\" (id, user_id, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent, metadata, created_at) " +
                        "VALUES (?, ?, ?::audit_action, ?, ?, ?::jsonb, ?::jsonb, ?::inet, ?, ?::jsonb, NOW())";
            UUID id = UUID.randomUUID();
            auditLog.setId(id);
            jdbcTemplate.update(sql,
                id,
                auditLog.getUserId() != null ? auditLog.getUserId() : null,
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId() != null ? auditLog.getEntityId() : null,
                auditLog.getOldValue() != null ? auditLog.getOldValue() : null,
                auditLog.getNewValue() != null ? auditLog.getNewValue() : null,
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getMetadata() != null ? auditLog.getMetadata() : "{}"
            );
        } else {
            String sql = "UPDATE \"audit_logs\" SET user_id = ?, action = ?::audit_action, entity_type = ?, entity_id = ?, " +
                        "old_value = ?::jsonb, new_value = ?::jsonb, ip_address = ?::inet, user_agent = ?, metadata = ?::jsonb WHERE id = ?";
            jdbcTemplate.update(sql,
                auditLog.getUserId() != null ? auditLog.getUserId() : null,
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId() != null ? auditLog.getEntityId() : null,
                auditLog.getOldValue() != null ? auditLog.getOldValue() : null,
                auditLog.getNewValue() != null ? auditLog.getNewValue() : null,
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getMetadata() != null ? auditLog.getMetadata() : "{}",
                auditLog.getId()
            );
        }
        return auditLog;
    }
    
    @Override
    public List<AuditLog> findByUserId(UUID userId, int page, int size) {
        String sql = "SELECT * FROM \"audit_logs\" WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, userId, size, page * size);
    }
    
    @Override
    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, int page, int size) {
        String sql = "SELECT * FROM \"audit_logs\" WHERE entity_type = ? AND entity_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, entityType, entityId, size, page * size);
    }
    
    @Override
    public List<AuditLog> findAll(int page, int size) {
        String sql = "SELECT * FROM \"audit_logs\" ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, size, page * size);
    }
}
