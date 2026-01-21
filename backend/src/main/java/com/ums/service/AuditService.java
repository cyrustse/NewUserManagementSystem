package com.ums.service;

import com.ums.dto.audit.AuditEvent;
import com.ums.dto.common.PageResponse;

import java.util.UUID;

public interface AuditService {

    void logAuditEvent(AuditEvent event);

    PageResponse<AuditEvent> getAuditLogs(int page, int size, String userId, String entityType, String entityId);

    AuditEvent getAuditLogById(UUID id);
}
