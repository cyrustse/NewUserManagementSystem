package com.ums.dto.role;

import jakarta.validation.constraints.Size;

public class UpdateRoleRequest {
    
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;
    
    private String description;
    
    private String parentId;
    
    private Integer priority;
    
    public UpdateRoleRequest() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
