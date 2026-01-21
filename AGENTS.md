# AGENTS.md - User Management System Development Guide

This document provides guidelines for agentic coding agents working on the User Management System.

## Project Overview

A full-stack application with Spring Boot backend (Java 17) and React/TypeScript frontend. The system includes JWT authentication, OPA policy engine, Redis caching, Kong API gateway, and PostgreSQL database.

## Build Commands

### Backend (Java/Spring Boot)
```bash
cd backend

# Build the application
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UserServiceTest

# Run a single test method
./mvnw test -Dtest=UserServiceTest#testCreateUser

# Start application (dev profile)
./mvnw spring-boot:run

# Build Docker image
docker build -t ums-backend:latest .
```

### Frontend (React/TypeScript)
```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Type-check only
npx tsc --noEmit

# Preview production build
npm run preview
```

### Docker Infrastructure
```bash
cd docker

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

## Code Style Guidelines

### Backend (Java)

**Package Structure:**
- `com.ums.config` - Configuration classes
- `com.ums.controller` - REST controllers
- `com.ums.dto` - Data transfer objects (request/response)
- `com.ums.entity` - JPA entities
- `com.ums.exception` - Custom exceptions and handlers
- `com.ums.repository` - Data access layer
- `com.ums.security` - Security-related code (JWT, filters)
- `com.ums.service` - Business logic
- `com.ums.util` - Utility classes

**Naming Conventions:**
- Classes: PascalCase (e.g., `UserService`, `CreateUserRequest`)
- Methods: camelCase (e.g., `findById`, `createUser`)
- Variables: camelCase (e.g., `userId`, `isActive`)
- Constants: SCREAMING_SNAKE_CASE (e.g., `DEFAULT_PAGE_SIZE`)
- DTOs: `{Entity}{Request|Response}` pattern (e.g., `UserResponse`, `CreateUserRequest`)

**DTO Pattern:**
- Use Jakarta Validation annotations (`@NotBlank`, `@Size`, `@Email`)
- Include validation messages (`message = "Username is required"`)
- Manual getters/setters (do not use Lombok unless class already uses it)
- Place DTOs in appropriate subdirectory under `dto/`

**API Response Format:**
Wrap all responses in `ApiResponse<T>`:
```java
// Success response
return ApiResponse.success(user);

// Success with message
return ApiResponse.success(user, "User created successfully");

// Error response
return ApiResponse.error("User not found");
```

**Error Handling:**
- Throw custom exceptions from `com.ums.exception`
- Use `@ControllerAdvice` for global exception handling
- Log errors with appropriate level (warn for business errors, error for system errors)

**Imports:**
- Group imports in this order: java.*, jakarta.*, org.*, com.*
- No wildcard imports except for testing

### Frontend (TypeScript)

**File Structure:**
```
src/
├── components/     # Reusable UI components
├── contexts/       # React contexts (Auth, Toast)
├── hooks/          # Custom React hooks
├── pages/          # Page components (Login, Dashboard, Users, Roles)
├── services/       # API service layer (Axios)
├── types/          # TypeScript type definitions
└── utils/          # Utility functions
```

**Naming Conventions:**
- Components: PascalCase (e.g., `UserTable.tsx`)
- Hooks: camelCase with "use" prefix (e.g., `useAuth.ts`)
- Utils: camelCase (e.g., `formatDate.ts`)
- Types: PascalCase (e.g., `UserResponse.ts`)
- Constants: SCREAMING_SNAKE_CASE (e.g., `API_BASE_URL`)

**TypeScript Strict Mode:**
- `noUnusedLocals: true` - Remove unused local variables
- `noUnusedParameters: true` - Remove unused function parameters
- `strict: true` - Enable all strict type checking
- Explicit return types for functions where beneficial

**React Patterns:**
- Use functional components with TypeScript
- Use `React.FC<Props>` for component typing
- Use custom hooks for reusable logic
- Use react-hook-form for forms
- Use react-toastify for notifications (via ToastContext)

**Imports:**
- React must be explicitly imported
- Group imports: React → external libraries → internal components/services
- Use absolute imports from `src/` (configured via tsconfig paths)

**Styling:**
- Use TailwindCSS utility classes
- No custom CSS files (use index.css for Tailwind directives only)
- Follow existing color/spacing patterns in components

## Development Workflow

### Running the Full Stack
1. Start infrastructure: `cd docker && docker-compose up -d`
2. Start backend: `cd backend && ./mvnw spring-boot:run`
3. Start frontend: `cd frontend && npm run dev`
4. Access: http://localhost:3000 (frontend), http://localhost:9090 (backend direct)

### Database Migrations
- Schema is in `database/schema.sql`
- Seed data includes default roles (SUPER_ADMIN, ADMIN, MANAGER, USER, VIEWER)
- Default admin: `admin` / `admin123`

### Environment Variables
- Backend: `backend/src/main/resources/application.yml`
- Frontend: Define in `.env` if needed
- Docker: `docker/.env` file

### Testing
- Backend: JUnit 5 + Spring Boot Test
- Frontend: No test framework configured (add if needed)

## Key Technologies
- **Backend**: Spring Boot 3.2.0, Java 17, PostgreSQL, Redis, JWT (jjwt 0.12.3), OPA, Kong
- **Frontend**: React 18, TypeScript 5, Vite 5, TailwindCSS 3, React Router 6, Axios, React Hook Form
- **Infrastructure**: Docker, Docker Compose

## Common Tasks

### Adding a New API Endpoint
1. Create DTOs in `backend/src/main/java/com/ums/dto/{entity}/`
2. Create/update Repository in `backend/src/main/java/com/ums/repository/`
3. Create Service in `backend/src/main/java/com/ums/service/`
4. Create Controller in `backend/src/main/java/com/ums/controller/`
5. Add frontend service in `frontend/src/services/`
6. Add frontend page/component in `frontend/src/pages/`

### Modifying Authentication
- JWT logic: `backend/src/main/java/com/ums/security/`
- Auth context: `frontend/src/contexts/AuthContext.tsx`
- Token handling: JWT access (30 min) and refresh (7 days)

### Modifying OPA Policies
- Policy files: `opa/` directory
- Endpoint: `http://localhost:8181/v1/data/authz/allow`
