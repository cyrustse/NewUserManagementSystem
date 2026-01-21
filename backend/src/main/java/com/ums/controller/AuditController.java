package com.ums.controller;

import com.ums.dto.audit.AuditEvent;
import com.ums.dto.common.ApiResponse;
import com.ums.dto.common.PageResponse;
import com.ums.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<ApiResponse<PageResponse<AuditEvent>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {

        PageResponse<AuditEvent> response = auditService.getAuditLogs(page, size, userId, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<ApiResponse<AuditEvent>> getAuditLog(@PathVariable String id) {
        try {
            UUID auditId = UUID.fromString(id);
            AuditEvent event = auditService.getAuditLogById(auditId);

            if (event == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponse.success(event));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid audit log ID format"));
        }
    }
}
