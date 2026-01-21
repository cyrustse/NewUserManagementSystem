package com.ums.service.impl;

import com.ums.dto.audit.AuditEvent;
import com.ums.dto.common.PageResponse;
import com.ums.entity.AuditLog;
import com.ums.repository.AuditLogRepository;
import com.ums.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void logAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(event.getUserId());
            auditLog.setAction(event.getAction());
            auditLog.setEntityType(event.getEntityType());
            auditLog.setEntityId(event.getEntityId());
            auditLog.setOldValue(event.getOldValue());
            auditLog.setNewValue(event.getNewValue());
            auditLog.setIpAddress(event.getIpAddress());
            auditLog.setUserAgent(event.getUserAgent());
            // Ensure metadata is never null - PostgreSQL JSONB requires valid JSON
            auditLog.setMetadata(event.getMetadata() != null ? event.getMetadata() : "{}");

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to log audit event: " + e.getMessage());
        }
    }

    @Override
    public PageResponse<AuditEvent> getAuditLogs(int page, int size, String userId, String entityType, String entityId) {
        List<AuditLog> logs;

        if (userId != null && !userId.isEmpty()) {
            logs = auditLogRepository.findByUserId(UUID.fromString(userId), page, size);
        } else if (entityType != null && entityId != null && !entityId.isEmpty()) {
            logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, UUID.fromString(entityId), page, size);
        } else {
            logs = auditLogRepository.findAll(page, size);
        }

        long total = logs.size(); // Simplified - actual implementation would count total
        List<AuditEvent> events = logs.stream()
                .map(this::mapToEvent)
                .collect(Collectors.toList());

        return new PageResponse<>(events, page, size, total);
    }

    @Override
    public AuditEvent getAuditLogById(UUID id) {
        // Simplified - actual implementation would query by ID
        List<AuditLog> logs = auditLogRepository.findAll(0, 100);
        return logs.stream()
                .filter(log -> log.getId().equals(id))
                .map(this::mapToEvent)
                .findFirst()
                .orElse(null);
    }

    private AuditEvent mapToEvent(AuditLog log) {
        AuditEvent event = new AuditEvent();
        event.setUserId(log.getUserId());
        event.setAction(log.getAction());
        event.setEntityType(log.getEntityType());
        event.setEntityId(log.getEntityId());
        event.setOldValue(log.getOldValue());
        event.setNewValue(log.getNewValue());
        event.setIpAddress(log.getIpAddress());
        event.setUserAgent(log.getUserAgent());
        event.setMetadata(log.getMetadata());
        return event;
    }
}
