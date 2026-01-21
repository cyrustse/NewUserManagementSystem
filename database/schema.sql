-- ============================================================
-- User Management System - Database Schema (PostgreSQL 15)
-- ============================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUMS
-- ============================================================
DO $$ BEGIN
    CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE resource_type AS ENUM ('API', 'MENU', 'BUTTON', 'DATA');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE permission_action AS ENUM ('CREATE', 'READ', 'UPDATE', 'DELETE', '*');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE scope_type AS ENUM ('ORGANIZATION', 'DEPARTMENT', 'TEAM', 'PROJECT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE audit_action AS ENUM ('CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'MFA_ENABLE', 'MFA_DISABLE', 'PASSWORD_CHANGE', 'ROLE_ASSIGN', 'ROLE_REVOKE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    status user_status DEFAULT 'PENDING' NOT NULL,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    last_login_at TIMESTAMP,
    login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted_at) WHERE deleted_at IS NULL;

-- ============================================================
-- ROLES TABLE (with hierarchical structure)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    parent_id UUID REFERENCES roles(id) ON DELETE SET NULL,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for roles
CREATE INDEX IF NOT EXISTS idx_roles_parent ON roles(parent_id);
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- ============================================================
-- RESOURCES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type resource_type NOT NULL,
    identifier VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES resources(id) ON DELETE SET NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for resources
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(type);
CREATE INDEX IF NOT EXISTS idx_resources_identifier ON resources(identifier);

-- ============================================================
-- PERMISSIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    resource_id UUID REFERENCES resources(id) ON DELETE CASCADE,
    action permission_action NOT NULL,
    conditions JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for permissions
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(resource_id);
CREATE INDEX IF NOT EXISTS idx_permissions_name ON permissions(name);

-- ============================================================
-- USER_ROLES TABLE (with scope support)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    scope VARCHAR(255),
    scope_type scope_type,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    granted_by UUID REFERENCES users(id),
    revoked_at TIMESTAMP
);

-- Indexes for user_roles
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_expires ON user_roles(expires_at);

-- ============================================================
-- ROLE_PERMISSIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(role_id, permission_id)
);

-- Indexes for role_permissions
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);

-- ============================================================
-- REFRESH_TOKENS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for refresh_tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at);

-- ============================================================
-- AUDIT_LOGS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action audit_action NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for audit_logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at DESC);

-- ============================================================
-- LOGIN_ATTEMPTS TABLE (for rate limiting)
-- ============================================================
CREATE TABLE IF NOT EXISTS login_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ip_address INET NOT NULL,
    username VARCHAR(255),
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    success BOOLEAN DEFAULT FALSE,
    failure_reason VARCHAR(255)
);

-- Indexes for login_attempts
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip ON login_attempts(ip_address, attempted_at);
CREATE INDEX IF NOT EXISTS idx_login_attempts_user ON login_attempts(username, attempted_at);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Insert system roles
INSERT INTO roles (id, name, description, is_system, priority) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'SUPER_ADMIN', 'System super administrator with all permissions', TRUE, 100),
('550e8400-e29b-41d4-a716-446655440001', 'ADMIN', 'Administrator with broad permissions', TRUE, 80),
('550e8400-e29b-41d4-a716-446655440002', 'MANAGER', 'Manager with department-level access', TRUE, 60),
('550e8400-e29b-41d4-a716-446655440003', 'USER', 'Standard user with basic permissions', TRUE, 40),
('550e8400-e29b-41d4-a716-446655440004', 'VIEWER', 'Read-only access', TRUE, 20)
ON CONFLICT (name) DO NOTHING;

-- Set role hierarchy
UPDATE roles SET parent_id = '550e8400-e29b-41d4-a716-446655440001' WHERE name = 'ADMIN' AND parent_id IS NULL;
UPDATE roles SET parent_id = '550e8400-e29b-41d4-a716-446655440002' WHERE name = 'MANAGER' AND parent_id IS NULL;
UPDATE roles SET parent_id = '550e8400-e29b-41d4-a716-446655440003' WHERE name = 'USER' AND parent_id IS NULL;
UPDATE roles SET parent_id = '550e8400-e29b-41d4-a716-446655440004' WHERE name = 'VIEWER' AND parent_id IS NULL;

-- Insert system resources
INSERT INTO resources (id, type, identifier, name, description) VALUES
('660e8400-e29b-41d4-a716-446655440000', 'API', '/api/v1/auth/**', 'Authentication API', 'All authentication endpoints'),
('660e8400-e29b-41d4-a716-446655440001', 'API', '/api/v1/users/**', 'User Management API', 'All user management endpoints'),
('660e8400-e29b-41d4-a716-446655440002', 'API', '/api/v1/roles/**', 'Role Management API', 'All role management endpoints'),
('660e8400-e29b-41d4-a716-446655440003', 'MENU', 'dashboard', 'Dashboard', 'Main dashboard menu'),
('660e8400-e29b-41d4-a716-446655440004', 'MENU', 'users', 'User Management', 'User management menu'),
('660e8400-e29b-41d4-a716-446655440005', 'MENU', 'roles', 'Role Management', 'Role management menu'),
('660e8400-e29b-41d4-a716-446655440006', 'MENU', 'settings', 'Settings', 'Settings menu')
ON CONFLICT (identifier) DO NOTHING;

-- Insert system permissions
INSERT INTO permissions (id, name, resource_id, action) VALUES
-- User permissions
('770e8400-e29b-41d4-a716-446655440000', 'user:create', '660e8400-e29b-41d4-a716-446655440001', 'CREATE'),
('770e8400-e29b-41d4-a716-446655440001', 'user:read', '660e8400-e29b-41d4-a716-446655440001', 'READ'),
('770e8400-e29b-41d4-a716-446655440002', 'user:update', '660e8400-e29b-41d4-a716-446655440001', 'UPDATE'),
('770e8400-e29b-41d4-a716-446655440003', 'user:delete', '660e8400-e29b-41d4-a716-446655440001', 'DELETE'),
('770e8400-e29b-41d4-a716-446655440004', 'user:*', '660e8400-e29b-41d4-a716-446655440001', '*'),
-- Role permissions
('770e8400-e29b-41d4-a716-446655440010', 'role:create', '660e8400-e29b-41d4-a716-446655440002', 'CREATE'),
('770e8400-e29b-41d4-a716-446655440011', 'role:read', '660e8400-e29b-41d4-a716-446655440002', 'READ'),
('770e8400-e29b-41d4-a716-446655440012', 'role:update', '660e8400-e29b-41d4-a716-446655440002', 'UPDATE'),
('770e8400-e29b-41d4-a716-446655440013', 'role:delete', '660e8400-e29b-41d4-a716-446655440002', 'DELETE'),
('770e8400-e29b-41d4-a716-446655440014', 'role:*', '660e8400-e29b-41d4-a716-446655440002', '*')
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Create default admin user (password: admin123)
INSERT INTO users (id, username, email, password_hash, status)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'admin',
    'admin@example.com',
    '$2b$12$Ci6DmREXts2.eHcgnlm7NeSXAODv/pNwg4zFN17AU0Lts7wTE4oAe',
    'ACTIVE'
)
ON CONFLICT (username) DO NOTHING;

-- Assign SUPER_ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.id, r.id, u.id FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

