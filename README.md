# User Management System

A full-stack User Management System built with Spring Boot, React, PostgreSQL, Redis, and Kong API Gateway.

## Features

- **User Management**: Create, read, update, delete users with status management
- **Role-Based Access Control**: Hierarchical role system with permissions
- **Permission Management**: Fine-grained permission controls tied to resources
- **Secure Authentication**: JWT-based auth with httpOnly cookies
- **Rate Limiting**: Redis-backed rate limiting to prevent abuse
- **Policy Engine**: Open Policy Agent (OPA) for authorization decisions

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | React + TypeScript + Tailwind CSS |
| Backend | Spring Boot 3.x + Java 17 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| API Gateway | Kong 3.5 |
| Policy Engine | Open Policy Agent (OPA) |
| Containerization | Docker + Docker Compose |

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │────▶│   Kong GW   │────▶│   Backend   │────▶│  PostgreSQL │
└─────────────┘     │ (JWT/Rate)  │     │ (Spring)    │     └─────────────┘
                    └─────────────┘     └─────────────┘
                           │                   │
                           ▼                   ▼
                      ┌─────────────┐     ┌─────────────┐
                      │    Redis    │     │     OPA     │
                      │ (Rate Limit)│     │  (Policy)   │
                      └─────────────┘     └─────────────┘
```

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd NewUserManagementSystem
   ```

2. **Start all services**
   ```bash
   cd docker
   docker compose up -d
   ```

3. **Access the application**
   - Frontend: http://localhost:3000
   - Kong Admin: http://localhost:8001

4. **Default Admin Credentials**
   - Username: `admin`
   - Password: `admin123`

### Services

| Service | Container | Port | Description |
|---------|-----------|------|-------------|
| Frontend | ums-frontend | 3000 | React SPA |
| API Gateway | ums-kong | 8000 | Kong Gateway |
| Backend | ums-backend | 9090 | Spring Boot API |
| Database | ums-postgres | 5432 | PostgreSQL |
| Cache | ums-redis | 6379 | Redis |
| Policy | ums-opa | 8181 | OPA Engine |

## Project Structure

```
NewUserManagementSystem/
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration
│   ├── pom.xml             # Maven config
│   └── Dockerfile
├── frontend/               # React application
│   ├── src/               # TypeScript source
│   ├── package.json       # NPM config
│   └── Dockerfile
├── docker/                # Docker Compose
│   └── docker-compose.yml
├── kong/                  # Kong configuration
│   └── kong.yml
├── opa/                   # OPA policies
│   └── policies/
├── database/              # Database schema
│   └── schema.sql
└── docs/                  # Documentation
    ├── architecture.md
    ├── SessionToken.md
    └── USER_GUIDE.md
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout

### Users
- `GET /api/v1/users` - List users (paginated)
- `POST /api/v1/users` - Create user
- `GET /api/v1/users/{id}` - Get user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user
- `POST /api/v1/users/{id}/roles` - Assign role
- `DELETE /api/v1/users/{id}/roles/{roleId}` - Remove role

### Roles
- `GET /api/v1/roles` - List roles
- `POST /api/v1/roles` - Create role
- `PUT /api/v1/roles/{id}` - Update role
- `DELETE /api/v1/roles/{id}` - Delete role

### Permissions
- `GET /api/v1/permissions` - List permissions
- `POST /api/v1/permissions` - Create permission
- `PUT /api/v1/permissions/{id}` - Update permission
- `DELETE /api/v1/permissions/{id}` - Delete permission

## Documentation

- [Architecture](docs/architecture.md) - System architecture diagrams
- [Session Tokens](docs/SessionToken.md) - JWT and cookie documentation
- [User Guide](USER_GUIDE.md) - End-user documentation

## License

MIT License
