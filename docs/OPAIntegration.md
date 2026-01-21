# OPA Authorization Integration Guide

## Overview

This document describes how to integrate Open Policy Agent (OPA) authorization across microservices written in different programming languages: **Java**, **Rust**, and **FastAPI (Python)**.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [OPA Server Setup](#opa-server-setup)
3. [Shared Policy Structure](#shared-policy-structure)
4. [Java Integration (Spring Boot)](#java-integration-spring-boot)
5. [FastAPI Integration (Python)](#fastapi-integration-python)
6. [Rust Integration](#rust-integration)
7. [Testing Authorization](#testing-authorization)
8. [Best Practices](#best-practices)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         OPA Policy Server                            │
│                     (Container: ums-opa :8181)                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  policies/                                                   │   │
│  │  ├── authz.rego         # Main authorization policy         │   │
│  │  ├── roles.rego         # Role hierarchy rules              │   │
│  │  └── resources.rego     # Resource access rules             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Endpoints:                                                         │
│  POST /v1/data/{package}/authz    ← Authorization checks           │
│  GET  /v1/policies                ← List loaded policies           │
│  POST /v1/compile                 ← Ad-hoc queries                 │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ HTTP/REST
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│   Java Svc    │         │  FastAPI Svc  │         │   Rust Svc    │
│   :8080       │         │   :8000       │         │   :9000       │
│               │         │               │         │               │
│ OpaService    │         │ opa-client    │         │ opa-rs        │
└───────────────┘         └───────────────┘         └───────────────┘
```

---

## OPA Server Setup

### Docker Configuration

Add OPA service to your Docker network:

```yaml
# docker-compose.yml (already configured)
opa:
  image: openpolicyagent/opa:latest
  container_name: ums-opa
  command: run --server --log-level debug
  ports:
    - "8181:8181"
  networks:
    - ums-network
  restart: unless-stopped
```

### Health Check

```bash
# Verify OPA is running
curl http://localhost:8181/health

# Response: {"alive": true}
```

---

## Shared Policy Structure

### Main Authorization Policy

Create a shared policy that all services can use:

```rego
# opa/policies/authz.rego
package authz

# Default deny all
default allow := false

# Allow if explicitly permitted
allow {
    input.action == "read"
    resource_has_permission(input.subject, input.resource, "read")
}

allow {
    input.action == "write"
    resource_has_permission(input.subject, input.resource, "write")
}

allow {
    input.action == "delete"
    resource_has_permission(input.subject, input.resource, "delete")
}

allow {
    input.action == "admin"
    resource_has_permission(input.subject, input.resource, "admin")
}

# Role-based permissions
allow {
    input.subject.roles[_] == "SUPER_ADMIN"
}

allow {
    input.subject.roles[_] == input.resource.admin_role
    input.action == "admin"
}

# Helper: Check if subject has required permission
resource_has_permission(subject, resource, action) {
    permission := subject.permissions[_]
    permission.resource == resource
    permission.action == action
}

resource_has_permission(subject, resource, action) {
    role := subject.roles[_]
    role_permissions := data.roles[role]
    permission := role_permissions[_]
    permission.resource == resource
    permission.action == action
}

# Ownership check - users can access their own data
allow {
    input.action == "read"
    input.resource == "users"
    input.subject.id == input.resource_id
}

allow {
    input.action == "write"
    input.resource == "users"
    input.subject.id == input.resource_id
}
```

### Role Hierarchy Policy

```rego
# opa/policies/roles.rego
package roles

# Role hierarchy - higher priority roles inherit lower ones
inherits_role(user_role, required_role) {
    hierarchy := {"SUPER_ADMIN": ["ADMIN", "EDITOR", "VIEWER"], "ADMIN": ["EDITOR", "VIEWER"], "EDITOR": ["VIEWER"]}
    hierarchy[user_role][_] == required_role
}

inherits_role(user_role, required_role) {
    user_role == required_role
}

# Check if user has role or inherits it
has_role(user_roles, required_role) {
    user_role := user_roles[_]
    inherits_role(user_role, required_role)
}
```

### Resource Definitions

```rego
# opa/policies/resources.rego
package resources

# Define resource types and their admin roles
admin_role["users"] := "ADMIN"
admin_role["roles"] := "SUPER_ADMIN"
admin_role["permissions"] := "SUPER_ADMIN"
admin_role["settings"] := "ADMIN"
```

### Bundle Configuration

```yaml
# opa/bundles.yaml
bundles:
  authz:
    version: 1.0
    served: true
    signing_key: ""
  roles:
    version: 1.0
    served: true
    signing_key: ""
```

---

## Java Integration (Spring Boot)

### Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### OPA Service

```java
// src/main/java/com/ums/service/OpaService.java
package com.ums.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class OpaService {

    @Value("${opa.url:http://localhost:8181}")
    private String opaUrl;

    private final WebClient webClient;

    public OpaService() {
        this.webClient = WebClient.builder()
            .baseUrl(opaUrl)
            .build();
    }

    public boolean evaluatePolicy(String userId, String resource, String action) {
        Map<String, Object> input = Map.of(
            "subject", Map.of(
                "id", userId
            ),
            "resource", resource,
            "action", action
        );

        Map<String, Object> body = Map.of("input", input);

        try {
            Map<String, Object> response = webClient.post()
                .uri("/v1/data/authz")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new org.springframework.coreParameterizedTypeReference<Map<String, Object>>() {})
                .block();

            return Boolean.TRUE.equals(response.get("result"));
        } catch (Exception e) {
            // Log error and deny by default
            return false;
        }
    }

    public boolean evaluatePolicyWithRoles(String userId, java.util.List<String> roles,
                                          String resource, String action) {
        Map<String, Object> input = Map.of(
            "subject", Map.of(
                "id", userId,
                "roles", roles
            ),
            "resource", resource,
            "action", action
        );

        Map<String, Object> body = Map.of("input", input);

        try {
            Map<String, Object> response = webClient.post()
                .uri("/v1/data/authz")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

            return Boolean.TRUE.equals(response.get("result"));
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Authorization Annotation

```java
// src/main/java/com/ums/security/OpaAuthorize.java
package com.ums.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpaAuthorize {
    String resource();
    String action() default "read";
}
```

### Authorization Interceptor

```java
// src/main/java/com/ums/security/OpaAuthorizationInterceptor.java
package com.ums.security;

import com.ums.service.OpaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OpaAuthorizationInterceptor implements HandlerInterceptor {

    @Autowired
    private OpaService opaService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        OpaAuthorize authz = method.getMethodAnnotation(OpaAuthorize.class);

        if (authz == null) {
            authz = method.getBeanType().getAnnotation(OpaAuthorize.class);
        }

        if (authz == null) {
            return true; // No authorization required
        }

        // Extract token from cookie
        String token = extractTokenFromCookie(request, "accessToken");
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // Get user ID from token
        String userId = jwtTokenProvider.getUserIdFromToken(token).toString();

        // Check authorization
        boolean allowed = opaService.evaluatePolicyWithRoles(
            userId,
            getUserRoles(token),
            authz.resource(),
            authz.action()
        );

        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private String extractTokenFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private java.util.List<String> getUserRoles(String token) {
        return jwtTokenProvider.validateToken(token).get("roles", java.util.List.class);
    }
}
```

### Usage Example

```java
// In your controller
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    @OpaAuthorize(resource = "users", action = "read")
    public ResponseEntity<List<User>> listUsers() {
        // Only authorized users can access
        return userService.findAll();
    }

    @PostMapping
    @OpaAuthorize(resource = "users", action = "write")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @DeleteMapping("/{id}")
    @OpaAuthorize(resource = "users", action = "delete")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        return userService.delete(UUID.fromString(id));
    }
}
```

---

## FastAPI Integration (Python)

### Dependency

```bash
pip install opa-client httpx
```

### OPA Client

```python
# opa_client.py
import httpx
from typing import List, Optional
from pydantic import BaseModel

class Subject(BaseModel):
    id: str
    roles: Optional[List[str]] = []
    permissions: Optional[List[dict]] = []

class AuthzInput(BaseModel):
    subject: Subject
    resource: str
    action: str
    resource_id: Optional[str] = None

class OpaClient:
    def __init__(self, base_url: str = "http://localhost:8181"):
        self.base_url = base_url

    async def check_authorization(
        self,
        user_id: str,
        roles: List[str],
        resource: str,
        action: str,
        resource_id: Optional[str] = None
    ) -> bool:
        input_data = {
            "input": {
                "subject": {
                    "id": user_id,
                    "roles": roles
                },
                "resource": resource,
                "action": action,
                "resource_id": resource_id
            }
        }

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/v1/data/authz",
                json=input_data
            )

            if response.status_code != 200:
                return False

            result = response.json()
            return result.get("result", False)

    async def check_permission(
        self,
        user_id: str,
        resource: str,
        action: str
    ) -> bool:
        """Simple permission check without roles."""
        input_data = {
            "input": {
                "subject": {"id": user_id},
                "resource": resource,
                "action": action
            }
        }

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/v1/data/authz",
                json=input_data
            )

            return response.json().get("result", False)

opa_client = OpaClient()
```

### FastAPI Dependency

```python
# authz.py
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import List
import jwt

security = HTTPBearer()

# Same secret as Java backend
JWT_SECRET = "your-jwt-secret-key"

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> dict:
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
        return {
            "id": payload["sub"],
            "roles": payload.get("roles", [])
        }
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired"
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token"
        )

class OpaAuthorize:
    def __init__(self, resource: str, action: str = "read"):
        self.resource = resource
        self.action = action

    async def __call__(self, user: dict = Depends(get_current_user)) -> dict:
        allowed = await opa_client.check_authorization(
            user_id=user["id"],
            roles=user["roles"],
            resource=self.resource,
            action=self.action
        )

        if not allowed:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access denied"
            )

        return user
```

### Usage Example

```python
# main.py
from fastapi import FastAPI, Depends
from authz import OpaAuthorize, get_current_user

app = FastAPI()

# Using dependency injection
@app.get("/items")
async def list_items(
    user: dict = Depends(OpaAuthorize(resource="items", action="read"))
):
    return {"items": ["item1", "item2"], "user": user["id"]}

@app.post("/items")
async def create_item(
    item: dict,
    user: dict = Depends(OpaAuthorize(resource="items", action="write"))
):
    return {"id": 1, **item}

@app.delete("/items/{item_id}")
async def delete_item(
    item_id: str,
    user: dict = Depends(OpaAuthorize(resource="items", action="delete"))
):
    return {"status": "deleted"}
```

---

## Rust Integration

### Dependency

```toml
# Cargo.toml
[dependencies]
opa-rs = "0.2"
reqwest = { version = "0.11", features = ["json"] }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
tokio = { version = "1.0", features = ["full"] }
```

### OPA Client

```rust
// src/opa_client.rs
use opa_rs::{OPAClient, InputBuilder};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Serialize)]
struct AuthzInput {
    input: InputData,
}

#[derive(Debug, Serialize)]
struct InputData {
    subject: Subject,
    resource: String,
    action: String,
    resource_id: Option<String>,
}

#[derive(Debug, Serialize)]
struct Subject {
    id: String,
    roles: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct AuthzResponse {
    result: bool,
}

pub struct OpaAuthzClient {
    client: Arc<OPAClient>,
}

impl OpaAuthzClient {
    pub fn new(opa_url: &str) -> Self {
        let client = OPAClient::new(opa_url.to_string());
        Self {
            client: Arc::new(client),
        }
    }

    pub async fn check_authorization(
        &self,
        user_id: &str,
        roles: &[String],
        resource: &str,
        action: &str,
    ) -> bool {
        let input = InputBuilder::new()
            .with("subject.id", user_id)
            .with("subject.roles", roles)
            .with("resource", resource)
            .with("action", action)
            .build();

        match self.client.eval("authz", input).await {
            Ok(result) => result.result,
            Err(e) => {
                eprintln!("OPA evaluation failed: {:?}", e);
                false
            }
        }
    }

    pub async fn check_permission(
        &self,
        user_id: &str,
        resource: &str,
        action: &str,
    ) -> bool {
        let input = InputBuilder::new()
            .with("subject.id", user_id)
            .with("resource", resource)
            .with("action", action)
            .build();

        match self.client.eval("authz", input).await {
            Ok(result) => result.result,
            Err(e) => {
                eprintln!("OPA evaluation failed: {:?}", e);
                false
            }
        }
    }
}

// Alternative: Using reqwest directly (no opa-rs dependency)
pub async fn check_authz_rest(
    opa_url: &str,
    user_id: &str,
    roles: &[String],
    resource: &str,
    action: &str,
) -> bool {
    let input = serde_json::json!({
        "input": {
            "subject": {
                "id": user_id,
                "roles": roles
            },
            "resource": resource,
            "action": action
        }
    });

    let client = reqwest::Client::new();
    let response = client
        .post(&format!("{}/v1/data/authz", opa_url))
        .json(&input)
        .send()
        .await
        .ok()
        .and_then(|r| r.json::<serde_json::Value>().await.ok());

    response
        .as_ref()
        .and_then(|v| v.get("result").and_then(|b| b.as_bool()))
        .unwrap_or(false)
}
```

### Axum Middleware

```rust
// src/middleware/authz.rs
use axum::{
    extract::{Request, State},
    middleware::{self, Next},
    response::Response,
};
use crate::opa_client::OpaAuthzClient;

#[derive(Clone)]
pub struct AuthState {
    opa_client: OpaAuthzClient,
    jwt_secret: String,
}

pub async fn authz_middleware(
    State(state): State<AuthState>,
    mut req: Request,
    next: Next,
) -> Result<Response, (http::StatusCode, String)> {
    // Extract token from header
    let token = req
        .headers()
        .get("Authorization")
        .and_then(|h| h.to_str().ok())
        .and_then(|h| h.strip_prefix("Bearer "));

    if token.is_none() {
        return Err((http::StatusCode::UNAUTHORIZED, "No token".to_string()));
    }

    // Decode JWT (simplified - use jsonwebtoken crate in production)
    let claims: serde_json::Value = match decode_jwt(token.unwrap(), &state.jwt_secret) {
        Ok(c) => c,
        Err(_) => return Err((http::StatusCode::UNAUTHORIZED, "Invalid token".to_string())),
    };

    let user_id = claims.get("sub").and_then(|v| v.as_str()).unwrap_or("");
    let roles: Vec<String> = claims
        .get("roles")
        .and_then(|v| v.as_array())
        .unwrap_or(&vec![])
        .iter()
        .filter_map(|v| v.as_str().map(String::from))
        .collect();

    // Extract resource and action from request
    let resource = extract_resource(&req);
    let action = extract_action(&req);

    // Check with OPA
    let allowed = state
        .opa_client
        .check_authorization(&user_id, &roles, &resource, &action)
        .await;

    if !allowed {
        return Err((http::StatusCode::FORBIDDEN, "Access denied".to_string()));
    }

    // Add user info to request extensions
    req.extensions_mut().insert(claims);

    Ok(next.run(req).await)
}

fn extract_resource(req: &Request) -> String {
    // Extract from path, e.g., /users -> "users"
    let path = req.uri().path();
    let segments: Vec<&str> = path.split('/').filter(|s| !s.is_empty()).collect();
    segments.first().map(|s| (*s).to_string()).unwrap_or_default()
}

fn extract_action(req: &Request) -> String {
    match req.method() {
        &http::Method::GET => "read".to_string(),
        &http::Method::POST => "write".to_string(),
        &http::Method::PUT => "write".to_string(),
        &http::Method::PATCH => "write".to_string(),
        &http::Method::DELETE => "delete".to_string(),
        _ => "read".to_string(),
    }
}

fn decode_jwt(token: &str, secret: &str) -> Result<serde_json::Value, ()> {
    // Use jsonwebtoken crate for actual decoding
    // This is a placeholder
    Ok(serde_json::json!({
        "sub": "user-123",
        "roles": ["VIEWER"]
    }))
}
```

### Usage Example

```rust
// src/main.rs
use axum::{routing::*, Router};
use crate::middleware::authz::{authz_middleware, AuthState};

#[tokio::main]
async fn main() {
    let auth_state = AuthState {
        opa_client: OpaAuthzClient::new("http://localhost:8181"),
        jwt_secret: "your-jwt-secret".to_string(),
    };

    let app = Router::new()
        .route("/users", get(list_users))
        .route("/users", post(create_user))
        .route_layer(middleware::from_fn_with_state(
            auth_state.clone(),
            authz_middleware,
        ))
        .with_state(auth_state);

    axum::Server::bind(&"0.0.0.0:9000".parse().unwrap())
        .serve(app.into_make_service())
        .await
        .unwrap();
}

async fn list_users() -> &'static str {
    "List of users"
}

async fn create_user() -> &'static str {
    "User created"
}
```

---

## Testing Authorization

### OPA Test Query

```bash
# Test authorization directly
curl -X POST http://localhost:8181/v1/data/authz \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "subject": {"id": "user-123", "roles": ["ADMIN"]},
      "resource": "users",
      "action": "read"
    }
  }'

# Response: {"result": true}

# Test denied access
curl -X POST http://localhost:8181/v1/data/authz \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "subject": {"id": "user-456", "roles": ["VIEWER"]},
      "resource": "users",
      "action": "delete"
    }
  }'

# Response: {"result": false}
```

### Unit Tests (OPA)

```rego
# opa/policies/authz_test.rego
package authz

test_allow_admin_read {
    allow with input as {
        "subject": {"id": "user-1", "roles": ["ADMIN"]},
        "resource": "users",
        "action": "read"
    }
}

test_deny_viewer_delete {
    not allow with input as {
        "subject": {"id": "user-2", "roles": ["VIEWER"]},
        "resource": "users",
        "action": "delete"
    }
}

test_super_admin_all {
    allow with input as {
        "subject": {"id": "super-1", "roles": ["SUPER_ADMIN"]},
        "resource": "roles",
        "action": "admin"
    }
}
```

```bash
# Run tests
opa test opa/policies/ -v
```

### Integration Test (Java)

```java
@MockBean
private OpaService opaService;

@Test
void testUserCreation_Authorized() {
    when(opaService.evaluatePolicyWithRoles(
        anyString(), anyList(), eq("users"), eq("write")
    )).thenReturn(true);

    // Test that creation works
}

@Test
void testUserCreation_Unauthorized() {
    when(opaService.evaluatePolicyWithRoles(
        anyString(), anyList(), eq("users"), eq("write")
    )).thenReturn(false);

    // Test that creation fails with 403
}
```

---

## Best Practices

### 1. Cache OPA Responses

```java
// In OpaService - cache decision for short duration
@Cacheable(value = "authz", key = "#userId + ':' + #resource + ':' + #action")
public boolean evaluatePolicy(...) {
    // Call OPA
}
```

### 2. Fail Closed

```rego
# Always deny by default
default allow := false
```

### 3. Logging

```java
// Log all authorization decisions
log.info("Authz decision: user={}, resource={}, action={}, allowed={}",
    userId, resource, action, allowed);
```

### 4. Batch Decisions

```rego
# For checking multiple resources at once
package batch_authz

allow_batch[resource] {
    resource := input.resources[_]
    input.subject.roles[_] == "ADMIN"
}
```

### 5. Performance

- Cache OPA responses for 30-60 seconds
- Use connection pooling for HTTP clients
- Consider OPA's partial evaluation for complex policies

---

## Quick Reference

| Language | Package | Import |
|----------|---------|--------|
| Java | Spring WebFlux | `WebClient` |
| Python | opa-client | `pip install opa-client` |
| Rust | opa-rs | `cargo add opa-rs` |
| Go | opa-sdk-go | `go get github.com/open-policy-agent/opa-sdk-go` |
| Node.js | @open-policy-agent/opa | `npm i @open-policy-agent/opa` |

### Environment Variables

```yaml
# All services should use these
OPA_URL: http://ums-opa:8181
```

### Health Check

```bash
# Verify OPA is healthy
curl http://localhost:8181/health
# {"alive": true}

# List loaded policies
curl http://localhost:8181/v1/policies
```
