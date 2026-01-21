package com.ums.repository;

import com.ums.entity.AuditLog;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    
    AuditLog save(AuditLog auditLog);
    
    List<AuditLog> findByUserId(UUID userId, int page, int size);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, int page, int size);
    
    List<AuditLog> findAll(int page, int size);
}
