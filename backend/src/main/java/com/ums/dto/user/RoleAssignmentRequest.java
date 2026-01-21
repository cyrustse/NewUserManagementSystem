package com.ums.dto.user;

import java.util.UUID;

public class RoleAssignmentRequest {
    
    private UUID roleId;
    private String scope;
    private String scopeType;
    
    public RoleAssignmentRequest() {}
    
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
}
