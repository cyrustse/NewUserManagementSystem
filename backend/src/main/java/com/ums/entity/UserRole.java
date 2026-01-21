package com.ums.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserRole {
    private UUID id;
    private UUID userId;
    private UUID roleId;
    private String scope;
    private String scopeType;  // ORGANIZATION, DEPARTMENT, TEAM, PROJECT
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private UUID grantedBy;
    private LocalDateTime revokedAt;
    
    public UserRole() {
        this.grantedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getRoleId() {
        return roleId;
    }
    
    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getScopeType() {
        return scopeType;
    }
    
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }
    
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public UUID getGrantedBy() {
        return grantedBy;
    }
    
    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public boolean isRevoked() {
        return revokedAt != null;
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
