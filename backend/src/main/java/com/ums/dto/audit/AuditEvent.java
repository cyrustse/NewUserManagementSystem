package com.ums.dto.audit;

import java.util.UUID;

public class AuditEvent {
    private UUID userId;
    private String action;
    private String entityType;
    private UUID entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    
    public AuditEvent() {}
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AuditEvent event = new AuditEvent();
        
        public Builder userId(UUID userId) {
            event.userId = userId;
            return this;
        }
        
        public Builder action(String action) {
            event.action = action;
            return this;
        }
        
        public Builder entityType(String entityType) {
            event.entityType = entityType;
            return this;
        }
        
        public Builder entityId(UUID entityId) {
            event.entityId = entityId;
            return this;
        }
        
        public Builder oldValue(String oldValue) {
            event.oldValue = oldValue;
            return this;
        }
        
        public Builder newValue(String newValue) {
            event.newValue = newValue;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }
        
        public Builder metadata(String metadata) {
            event.metadata = metadata;
            return this;
        }
        
        public AuditEvent build() {
            return event;
        }
    }
    
    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
