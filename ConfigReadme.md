# User Management System - Configuration Guide

This document provides comprehensive information about all configuration options available in the User Management System.

## Table of Contents

1. [Configuration Files Overview](#configuration-files-overview)
2. [Backend Configuration](#backend-configuration)
   - [Database Settings](#database-settings)
   - [JWT Settings](#jwt-settings)
   - [Redis Settings](#redis-settings)
   - [OPA Settings](#opa-settings)
   - [Security Settings](#security-settings)
   - [Rate Limiting](#rate-limiting)
3. [Docker Compose Configuration](#docker-compose-configuration)
4. [Kong Gateway Configuration](#kong-gateway-configuration)
5. [Database Schema Configuration](#database-schema-configuration)
6. [Environment Variables](#environment-variables)
7. [Environment-Specific Configuration](#environment-specific-configuration)
8. [Quick Reference](#quick-reference)
9. [Examples](#examples)

---

## Configuration Files Overview

The application uses multiple configuration files for different components:

```
NewUserManagementSystem/
├── backend/src/main/resources/
│   └── application.yml              # Main backend configuration
├── docker/
│   └── docker-compose.yml           # Infrastructure services
├── kong/
│   └── kong.yml                     # Kong gateway configuration
└── database/
    └── schema.sql                   # Database schema and seed data
```

---

## Backend Configuration

**File**: `backend/src/main/resources/application.yml`

This is the main configuration file for the Spring Boot backend application.

### Database Settings

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:ums}
    username: ${DB_USERNAME:ums_user}
    password: ${DB_PASSWORD:ums_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20           # Maximum connection pool size
      minimum-idle: 5                 # Minimum idle connections
      idle-timeout: 300000            # Idle connection timeout (ms)
      connection-timeout: 20000       # Connection acquisition timeout (ms)
```

#### Database Configuration Options

| Parameter | Environment Variable | Default | Description |
|-----------|---------------------|---------|-------------|
| `url` | `DB_HOST`, `DB_PORT`, `DB_NAME` | `jdbc:postgresql://localhost:5432/ums` | JDBC connection URL |
| `username` | `DB_USERNAME` | `ums_user` | Database username |
| `password` | `DB_PASSWORD` | `ums_password` | Database password |
| `port` | `DB_PORT` | `5432` | Database port |
| `maximum-pool-size` | - | `20` | Maximum pool connections |
| `minimum-idle` | - | `5` | Minimum idle connections |
| `idle-timeout` | - | `300000` | Idle timeout in milliseconds |
| `connection-timeout` | - | `20000` | Connection timeout in milliseconds |

#### Example Database URL Formats

```yaml
# Local development
spring.datasource.url: jdbc:postgresql://localhost:5432/ums

# Docker container
spring.datasource.url: jdbc:postgresql://postgres:5432/ums

# AWS RDS
spring.datasource.url: jdbc:postgresql://db.example.com:5432/prod_ums

# With SSL
spring.datasource.url: jdbc:postgresql://db.example.com:5432/prod_ums?sslmode=require
```

---

### JWT Settings

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-for-jwt-signing-here-must-be-at-least-256-bits}
  access-token-expiration: 1800     # 30 minutes in seconds
  refresh-token-expiration: 604800   # 7 days in seconds
  issuer: ${JWT_ISSUER:user-management-system}
```

#### JWT Configuration Options

| Parameter | Environment Variable | Default | Description |
|-----------|---------------------|---------|-------------|
| `secret` | `JWT_SECRET` | 256-bit secret key | HMAC-SHA256 signing key (minimum 256 bits) |
| `access-token-expiration` | - | `1800` | Access token validity in seconds (30 minutes) |
| `refresh-token-expiration` | - | `604800` | Refresh token validity in seconds (7 days) |
| `issuer` | `JWT_ISSUER` | `user-management-system` | JWT issuer claim |

#### JWT Token Claims

The system generates two types of tokens:

**Access Token**:
```json
{
  "sub": "user-id",
  "username": "admin",
  "email": "admin@example.com",
  "roles": ["SUPER_ADMIN"],
  "role_priorities": [100],
  "iss": "user-management-system",
  "iat": 1705689600,
  "exp": 1705691400
}
```

**Refresh Token**:
```json
{
  "sub": "user-id",
  "type": "refresh",
  "iss": "user-management-system",
  "iat": 1705689600,
  "exp": 1706294400
}
```

#### Security Best Practices for JWT Secret

⚠️ **IMPORTANT**: The JWT secret must be:

1. **At least 256 bits (32 characters)**
2. **Random and unpredictable**
3. **Stored securely** (environment variables, secrets manager)
4. **Never hardcoded** in production

**Generate a secure secret**:
```bash
# Using openssl
openssl rand -base64 64

# Using Java
java -jar -cp "lib/*" GenerateSecret 256

# Example of a strong secret
your-256-bit-secret-key-for-jwt-signing-here-must-be-at-least-256-bits
```

---

### Redis Settings

```yaml
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD:}
  timeout: 2000
  cache:
    ttl: 3600                        # Default cache TTL in seconds
```

#### Redis Configuration Options

| Parameter | Environment Variable | Default | Description |
|-----------|---------------------|---------|-------------|
| `host` | `REDIS_HOST` | `localhost` | Redis server hostname |
| `port` | `REDIS_PORT` | `6379` | Redis server port |
| `password` | `REDIS_PASSWORD` | (empty) | Redis authentication password |
| `timeout` | - | `2000` | Connection timeout in milliseconds |
| `cache.ttl` | - | `3600` | Default cache TTL in seconds (1 hour) |

#### Redis Use Cases

The system uses Redis for:

1. **Session Caching**: Store active user sessions
2. **Token Blacklisting**: Revoke tokens on logout
3. **Rate Limiting Counters**: Track request counts per IP/user
4. **OPA Decision Caching**: Cache policy decisions
5. **User Data Caching**: Cache frequently accessed user data

#### Redis Commands for Monitoring

```bash
# Connect to Redis CLI
redis-cli

# View all keys
KEYS *

# View session data
GET session:user-id

# View rate limit counter
GET ratelimit:auth:ip:192.168.1.1

# Monitor Redis operations
MONITOR

# Check memory usage
INFO memory
```

---

### OPA Settings

```yaml
opa:
  url: ${OPA_URL:http://localhost:8181}
  policy:
    bundle: authz
    decision: allow
  cache:
    ttl: 300                         # OPA decision cache TTL in seconds
```

#### OPA Configuration Options

| Parameter | Environment Variable | Default | Description |
|-----------|---------------------|---------|-------------|
| `url` | `OPA_URL` | `http://localhost:8181` | OPA server URL |
| `policy.bundle` | - | `authz` | Policy bundle name |
| `policy.decision` | - | `allow` | Decision rule name |
| `cache.ttl` | - | `300` | Policy decision cache TTL (5 minutes) |

#### OPA Decision Endpoint

The system calls OPA's decision endpoint:

```
POST http://opa:8181/v1/data/authz/allow
Content-Type: application/json

{
  "subject": {
    "id": "user-id",
    "roles": ["USER", "MANAGER"],
    "department": "engineering"
  },
  "resource": "/api/v1/users",
  "action": "READ",
  "context": {
    "time": "2024-01-15T14:30:00Z",
    "ip_address": "192.168.1.100"
  }
}
```

#### OPA Admin API

```bash
# Check OPA health
curl http://localhost:8181/health

# List policies
curl http://localhost:8181/v1/policies

# Get policy decision
curl -X POST http://localhost:8181/v1/data/authz/allow \
  -H "Content-Type: application/json" \
  -d '{"input": {"subject": {"roles": ["USER"]}, "resource": "/api/v1/users", "action": "READ"}}'

# Reload policies
curl -X POST http://localhost:8181/v1/reload
```

---

### Security Settings

```yaml
security:
  bcrypt:
    strength: 12                     # BCrypt work factor (4-31)
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
  mfa:
    issuer: ${MFA_ISSUER:UserManagementSystem}
```

#### Security Configuration Options

| Parameter | Environment Variable | Default | Description |
|-----------|---------------------|---------|-------------|
| `bcrypt.strength` | - | `12` | BCrypt work factor (recommended: 10-12) |
| `cors.allowed-origins` | `CORS_ORIGINS` | `http://localhost:3000` | Allowed CORS origins |
| `cors.allowed-methods` | - | `GET,POST,PUT,DELETE,OPTIONS` | Allowed HTTP methods |
| `cors.allowed-headers` | - | `*` | Allowed headers |
| `cors.allow-credentials` | - | `true` | Allow credentials (cookies, auth headers) |
| `mfa.issuer` | `MFA_ISSUER` | `UserManagementSystem` | TOTP issuer name |

#### BCrypt Work Factor

The BCrypt work factor (strength) determines the computational cost:

| Work Factor | Time per Hash | Security Level |
|-------------|---------------|----------------|
| 4 | ~60ms | Weak (not recommended) |
| 8 | ~250ms | Basic |
| 10 | ~1s | Standard |
| **12** | ~4s | **Recommended** |
| 14 | ~16s | Strong (slow) |

⚠️ **Note**: Higher work factors increase security but also increase login time.

#### CORS Configuration

The CORS settings control cross-origin requests from the frontend:

```yaml
# Allow specific origins
security:
  cors:
    allowed-origins:
      - http://localhost:3000        # Development
      - https://app.example.com      # Production

# Allow all methods
security:
  cors:
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - PATCH
      - OPTIONS

# Allow specific headers
security:
  cors:
    allowed-headers:
      - Authorization
      - Content-Type
      - X-Request-ID
```

---

### Rate Limiting

```yaml
rate-limit:
  auth:
    requests: 20
    window: 60                       # per minute
  api:
    requests: 60
    window: 60                       # per minute
  ip-limit:
    max-attempts: 5
    window: 3600                     # per hour
```

#### Rate Limiting Configuration Options

| Parameter | Default | Description |
|-----------|---------|-------------|
| `auth.requests` | `20` | Maximum requests for auth endpoints |
| `auth.window` | `60` | Time window in seconds (1 minute) |
| `api.requests` | `60` | Maximum requests for API endpoints |
| `api.window` | `60` | Time window in seconds (1 minute) |
| `ip-limit.max-attempts` | `5` | Max failed login attempts per hour |
| `ip-limit.window` | `3600` | Time window in seconds (1 hour) |

#### Rate Limit Behavior

**Auth Endpoints** (`/auth/login`, `/auth/refresh`, `/auth/logout`):
- Maximum 20 requests per minute per IP
- After limit: Returns HTTP 429 (Too Many Requests)

**API Endpoints** (all other endpoints):
- Maximum 60 requests per minute per user
- After limit: Returns HTTP 429

**Login Protection**:
- Maximum 5 failed attempts per hour per IP
- After limit: Account locked for 1 hour

#### Rate Limit Response Headers

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705689600
Retry-After: 60
```

---

## Docker Compose Configuration

**File**: `docker/docker-compose.yml`

This file configures all infrastructure services.

### Service Ports

| Service | Container Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| PostgreSQL | 5432 | 5432 | Primary database |
| Redis | 6379 | 6379 | Cache and sessions |
| OPA | 8181 | 8181 | Policy engine |
| Kong Proxy | 8000 | 80 | HTTP gateway |
| Kong Admin | 8001 | 8001 | Admin API |
| Backend | 8080 | 8080 | Application server |

### Environment Variables in Docker Compose

```yaml
backend:
  environment:
    SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE:-dev}
    DB_HOST: postgres
    DB_PORT: 5432
    DB_NAME: ums
    DB_USERNAME: ${DB_USERNAME:-ums_user}
    DB_PASSWORD: ${DB_PASSWORD:-ums_password}
    REDIS_HOST: redis
    REDIS_PORT: 6379
    OPA_URL: ${OPA_URL:-http://opa:8181}
    JWT_SECRET: ${JWT_SECRET}
    JWT_ISSUER: ${JWT_ISSUER:-user-management-system}
```

### Docker Volume Configuration

```yaml
volumes:
  postgres_data:                    # PostgreSQL data persistence
    driver: local
  redis_data:                       # Redis data persistence
    driver: local
  opa_data:                         # OPA data persistence
    driver: local
  kong_data:                        # Kong data persistence
    driver: local
```

### Docker Compose Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f postgres
docker-compose logs -f redis
docker-compose logs -f opa
docker-compose logs -f kong
docker-compose logs -f backend

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Rebuild services
docker-compose up -d --build

# Scale a service
docker-compose up -d --scale backend=3
```

---

## Kong Gateway Configuration

**File**: `kong/kong.yml`

### Kong Services Configuration

```yaml
services:
  - name: auth-service
    url: http://auth-service:8080
    retries: 3
    connect_timeout: 10000
    write_timeout: 60000
    read_timeout: 60000
```

### Kong Plugins Configuration

#### JWT Plugin

```yaml
- name: jwt
  config:
    key_claim_name: iss              # JWT claim for key identification
    claims_to_verify:                # Claims to validate
      - exp                          # Expiration time
    run_on_preflight: true          # Run on OPTIONS requests
    maximum_expiration: 0            # Max token lifetime (0 = no limit)
    anon_tokens: []                  # Anonymous token patterns
```

#### Rate Limiting Plugin

```yaml
- name: rate-limiting
  config:
    minute: 20                       # Requests per minute
    hour: 0                          # Requests per hour (0 = unlimited)
    policy: local                    # Policy: local, cluster, or redis
    fault_tolerant: true             # Continue if Redis fails
    hide_client_headers: false       # Show rate limit headers
```

#### CORS Plugin

```yaml
- name: cors
  config:
    origins:
      - http://localhost:3000       # Allowed origins
      - https://app.example.com
    methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    headers:                        # Allowed headers
      - Accept
      - Accept-Version
      - Content-Length
      - Content-Type
      - Authorization
    exposed_headers:                # Headers exposed to client
      - X-Request-ID
    credentials: true               # Allow credentials
    max_age: 3600                   # Preflight cache duration
```

#### Request Size Limiting Plugin

```yaml
- name: request-size-limiting
  config:
    allowed_payload_size: 128       # Maximum request size
    size_unit: megabytes            # Size unit: bytes or megabytes
```

---

## Database Schema Configuration

**File**: `database/schema.sql`

### User Status Enum

```sql
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING');
```

| Status | Description |
|--------|-------------|
| `ACTIVE` | User can log in and use the system |
| `INACTIVE` | User account is deactivated |
| `LOCKED` | User account is temporarily locked (e.g., too many failed attempts) |
| `PENDING` | User account is pending activation/verification |

### Resource Types

```sql
CREATE TYPE resource_type AS ENUM ('API', 'MENU', 'BUTTON', 'DATA');
```

| Type | Description |
|------|-------------|
| `API` | API endpoints (e.g., `/api/v1/users`) |
| `MENU` | Menu items in the UI |
| `BUTTON` | UI buttons |
| `DATA` | Data resources |

### Permission Actions

```sql
CREATE TYPE permission_action AS ENUM ('CREATE', 'READ', 'UPDATE', 'DELETE', '*');
```

| Action | Description |
|--------|-------------|
| `CREATE` | Create new resources |
| `READ` | Read/retrieve resources |
| `UPDATE` | Update existing resources |
| `DELETE` | Delete resources |
| `*` | All actions |

### Seed Data Configuration

#### Default Roles

```sql
-- SUPER_ADMIN: Full system access (priority: 100)
INSERT INTO roles (name, description, is_system, priority)
VALUES ('SUPER_ADMIN', 'System super administrator', TRUE, 100);

-- ADMIN: Broad permissions (priority: 80)
INSERT INTO roles (name, description, is_system, priority)
VALUES ('ADMIN', 'Administrator with broad permissions', TRUE, 80);

-- MANAGER: Department-level access (priority: 60)
INSERT INTO roles (name, description, is_system, priority)
VALUES ('MANAGER', 'Manager with department-level access', TRUE, 60);

-- USER: Basic permissions (priority: 40)
INSERT INTO roles (name, description, is_system, priority)
VALUES ('USER', 'Standard user with basic permissions', TRUE, 40);

-- VIEWER: Read-only access (priority: 20)
INSERT INTO roles (name, description, is_system, priority)
VALUES ('VIEWER', 'Read-only access', TRUE, 20);
```

#### Role Hierarchy

```sql
-- Role inheritance structure
UPDATE roles SET parent_id = 'admin-role-id' WHERE name = 'ADMIN';
UPDATE roles SET parent_id = 'manager-role-id' WHERE name = 'MANAGER';
UPDATE roles SET parent_id = 'user-role-id' WHERE name = 'USER';
UPDATE roles SET parent_id = 'viewer-role-id' WHERE name = 'VIEWER';
```

#### Default Admin User

```sql
-- Default admin credentials: admin / admin123
INSERT INTO users (username, email, password_hash, status)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4aYJGYxMnC6C5.Oy',  -- BCrypt hash of "admin123"
    'ACTIVE'
);
```

**⚠️ Security Note**: Change the default admin password immediately after first login!

---

## Environment Variables

You can override all configuration settings using environment variables.

### Complete Environment Variable Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | Database server hostname |
| `DB_PORT` | `5432` | Database server port |
| `DB_NAME` | `ums` | Database name |
| `DB_USERNAME` | `ums_user` | Database username |
| `DB_PASSWORD` | `ums_password` | Database password |
| `JWT_SECRET` | 256-bit secret | JWT signing secret |
| `JWT_ISSUER` | `user-management-system` | JWT issuer |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `1800` | Access token validity (seconds) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `604800` | Refresh token validity (seconds) |
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_PASSWORD` | (empty) | Redis password |
| `OPA_URL` | `http://localhost:8181` | OPA server URL |
| `SERVER_PORT` | `8080` | Backend server port |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed CORS origins |
| `MFA_ISSUER` | `UserManagementSystem` | TOTP issuer name |
| `SPRING_PROFILE` | `dev` | Spring profile (dev, prod) |

### Setting Environment Variables

#### Linux/macOS

```bash
# Temporary (current session)
export DB_PASSWORD=mysecretpassword
export JWT_SECRET=myjwtsecret

# Permanent (add to ~/.bashrc or ~/.zshrc)
echo 'export DB_PASSWORD=mysecretpassword' >> ~/.bashrc
echo 'export JWT_SECRET=myjwtsecret' >> ~/.bashrc
source ~/.bashrc
```

#### Docker Compose

Create a `.env` file in the `docker/` directory:

```bash
# docker/.env
DB_PASSWORD=mysecretpassword
JWT_SECRET=myjwtsecret
CORS_ORIGINS=http://localhost:3000,https://app.example.com
SPRING_PROFILE=prod
```

#### Kubernetes

```yaml
# deployment.yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: jwt-secret
        key: secret
```

---

## Environment-Specific Configuration

### Development Environment

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ums_dev
    username: dev_user
    password: dev_password

jwt:
  secret: dev-secret-key-change-in-production
  access-token-expiration: 1800
  refresh-token-expiration: 604800

security:
  bcrypt:
    strength: 10                    # Faster for development
  cors:
    allowed-origins: http://localhost:3000

rate-limit:
  auth:
    requests: 20
  api:
    requests: 60
```

### Production Environment

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db.example.com:5432/prod_ums
    username: prod_user
    password: ${DB_PASSWORD}        # From environment variable

jwt:
  secret: ${JWT_SECRET}             # Must be at least 256 bits
  access-token-expiration: 1800     # 30 minutes
  refresh-token-expiration: 604800  # 7 days

security:
  bcrypt:
    strength: 12                    # More secure for production
  cors:
    allowed-origins: https://app.example.com

rate-limit:
  auth:
    requests: 20                    # Stricter limits in production
  api:
    requests: 100                   # Allow more legitimate traffic
```

### Docker Compose for Different Environments

```yaml
# docker-compose.prod.yml
services:
  postgres:
    environment:
      POSTGRES_USER: ${PROD_DB_USER}
      POSTGRES_PASSWORD: ${PROD_DB_PASSWORD}
    volumes:
      - prod_postgres_data:/var/lib/postgresql/data

  backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: prod_ums
      DB_USERNAME: ${PROD_DB_USER}
      DB_PASSWORD: ${PROD_DB_PASSWORD}
      REDIS_HOST: redis
      JWT_SECRET: ${PROD_JWT_SECRET}
```

---

## Quick Reference

### Port Reference

| Service | Default Port | Environment Variable |
|---------|-------------|---------------------|
| Backend API | 8080 | `SERVER_PORT` |
| PostgreSQL | 5432 | `DB_PORT` |
| Redis | 6379 | `REDIS_PORT` |
| OPA | 8181 | - |
| Kong Proxy | 80 | - |
| Kong Admin | 8001 | - |

### Timeout Reference

| Component | Default | Configuration |
|-----------|---------|---------------|
| Access Token | 30 minutes | `jwt.access-token-expiration` |
| Refresh Token | 7 days | `jwt.refresh-token-expiration` |
| MFA Temp Token | 5 minutes | Hardcoded in `JwtTokenProvider` |
| DB Connection | 20 seconds | `spring.datasource.hikari.connection-timeout` |
| DB Idle | 5 minutes | `spring.datasource.hikari.idle-timeout` |
| Redis Operation | 2 seconds | `redis.timeout` |
| OPA Cache | 5 minutes | `opa.cache.ttl` |
| Rate Limit Window | 60 seconds | `rate-limit.*.window` |

### Security Defaults

| Feature | Default | Recommendation |
|---------|---------|----------------|
| BCrypt Work Factor | 12 | 10-12 for balance of security/speed |
| JWT Access Token | 30 minutes | 15-30 minutes for sensitive apps |
| JWT Refresh Token | 7 days | 7-30 days |
| Rate Limit (Auth) | 20/min | 10-20 for sensitive endpoints |
| Rate Limit (API) | 60/min | 60-100 for normal usage |
| Login Attempts | 5/hour | 3-5 for security |
| Lockout Duration | 1 hour | 15-60 minutes |

---

## Examples

### Complete Docker Environment File

```bash
# docker/.env

# Database Configuration
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ums
DB_USERNAME=ums_admin
DB_PASSWORD=SecurePassword123!

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=RedisPassword456!

# JWT Configuration
JWT_SECRET=YourSuperSecretKeyMustBeAtLeast256BitsLong!!!
JWT_ISSUER=user-management-system
JWT_ACCESS_TOKEN_EXPIRATION=1800
JWT_REFRESH_TOKEN_EXPIRATION=604800

# OPA Configuration
OPA_URL=http://opa:8181

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILE=dev

# CORS Configuration
CORS_ORIGINS=http://localhost:3000,https://dev-app.example.com

# MFA Configuration
MFA_ISSUER=UserManagementSystem-Dev
```

### Kubernetes Secret Configuration

```yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: ums-secrets
type: Opaque
stringData:
  # Database credentials
  DB_PASSWORD: "YourDatabasePassword123!"
  
  # JWT secret (must be at least 256 bits)
  JWT_SECRET: "YourJWTSecretKeyMustBeAtLeast256BitsLongForSecurity!!!"
  
  # Redis password
  REDIS_PASSWORD: "YourRedisPassword789!"
  
  # Admin credentials (base64 encoded)
  ADMIN_PASSWORD: "YWRtaW4xMjM="  # "admin123" in base64
```

### Application Properties Override

You can create environment-specific property files:

```yaml
# application-prod.yml
spring:
  config:
    import: optional:file:./secrets.yml

jwt:
  secret: ${JWT_SECRET}
  
logging:
  level:
    root: INFO
    com.ums: WARN
```

---

## Troubleshooting

### Common Configuration Issues

#### 1. Database Connection Failed

```
Cause: Wrong database credentials or host
Solution:
- Verify DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- Check if PostgreSQL is running: docker-compose ps postgres
- Check logs: docker-compose logs postgres
```

#### 2. JWT Token Invalid

```
Cause: JWT secret mismatch or expired token
Solution:
- Verify JWT_SECRET is the same across all instances
- Check token expiration: jwtTokenProvider.validateToken(token)
- Generate new tokens after secret change
```

#### 3. CORS Error

```
Cause: Frontend origin not allowed
Solution:
- Add frontend URL to CORS_ORIGINS
- Check for typos in allowed origins
- Verify CORS configuration in both backend and Kong
```

#### 4. Rate Limiting Blocking Requests

```
Cause: Too many requests in time window
Solution:
- Wait for rate limit window to reset
- Check rate limit headers for reset time
- Implement request queuing in frontend
```

#### 5. OPA Policy Not Loading

```
Cause: OPA server not running or policy error
Solution:
- Check OPA health: curl http://localhost:8181/health
- Verify policy.rego syntax: opa check /opa/policies/policy.rego
- Check OPA logs: docker-compose logs opa
```

---

## Additional Resources

### Related Documentation

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/) - JWT Debugger
- [Kong Gateway Documentation](https://docs.konghq.com/gateway/)
- [OPA Documentation](https://www.openpolicyagent.org/docs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)

---

## Support

For configuration issues or questions:

1. Check the application logs: `docker-compose logs backend`
2. Verify all environment variables are set correctly
3. Ensure all services are running: `docker-compose ps`
4. Check individual service health endpoints

---

**Last Updated**: January 2024
**Version**: 1.0.0
