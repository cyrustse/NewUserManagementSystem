package authz

import input.subject
import input.resource
import input.action
import input.context

# Default deny
default allow = false

# Allow if explicitly permitted
allow {
    input.allowed == true
}

# Super admin has all permissions
allow {
    is_super_admin
    resource != "/auth/login"
}

is_super_admin {
    subject.roles[_] == "SUPER_ADMIN"
}

# Check if user has required permission
has_permission(role, permission) {
    permission_role[permission][role]
}

has_permission(role, permission) {
    permission_role[permission][parent]
    role_parent[role][parent]
}

# Role hierarchy
role_parent[child] = parent {
    data.roles[role]
    role.id == child
    role.parent_id == parent
}

role_hierarchy[root] := all_ancestors {
    some role in data.roles
    role.name == root
    role.parent_id != null
    
    parent_role in data.roles
    parent_role.id == role.parent_id
    
    all_ancestors = [parent_role.name] + role_hierarchy[parent_role.name]
}

role_hierarchy[root] := [] {
    some role in data.roles
    role.name == root
    role.parent_id == null
}

# Role-Permission mapping
permission_role[permission_name] = role_names {
    some permission in data.permissions
    permission.name == permission_name
    
    some role_permission in data.role_permissions
    role_permission.permission_id == permission.id
    
    some role in data.roles
    role.id == role_permission.role_id
    
    role_names = [role.name]
}

# Check department scope
has_department_scope(user_id, action, resource_type) {
    some user_role in subject.roles
    user_role == "MANAGER"
    
    user_department := context.user_department
    resource_department := context.resource_department
    
    user_department == resource_department
}

# Resource access based on type
can_access_api(resource_path) {
    resource := data.resources[_]
    resource.type == "API"
    resource.identifier == resource_path
    has_action(resource.action, input.action)
}

has_action(resource_action, requested_action) {
    resource_action == "*"
}

has_action(resource_action, requested_action) {
    resource_action == requested_action
}

# ABAC conditions
user_can_access(user, resource, action) {
    not user.status == "LOCKED"
    not user.status == "INACTIVE"
    user.status == "ACTIVE"
    
    some role in user.roles
    role in resource.allowed_roles
}

user_can_access(user, resource, action) {
    user.status == "ACTIVE"
    some role in user.roles
    role == "SUPER_ADMIN"
}
