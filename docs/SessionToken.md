# Session Token Documentation

## Overview

This document describes the session token architecture for the User Management System, including JWT token structure, cookie storage mechanism, and security features.

---

## Token Architecture

The system uses a **dual-token JWT architecture**:

| Token Type | Purpose | Expiration | Stored In |
|------------|---------|------------|-----------|
| **Access Token** | Authenticated API requests | 15 minutes | httpOnly cookie |
| **Refresh Token** | Get new access tokens | 7 days | httpOnly cookie |

### Token Flow

```
1. User logs in
   └─→ Server returns BOTH tokens in httpOnly cookies
   └─→ Browser stores cookies automatically

2. API requests
   └─→ Browser sends cookies automatically with each request
   └─→ Kong validates JWT signature
   └─→ Request proceeds if valid

3. Access token expires
   └─→ Frontend detects 401 response
   └─→ Calls /auth/refresh endpoint
   └──→ Server validates refresh token
   └─→ Returns new access token cookie
   └─→ Retry original request

4. User logs out
   └─→ Server revokes refresh token
   └─→ Both cookies cleared with maxAge=0
```

---

## JWT Token Structure

### Access Token

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "666e8600-0234-5678-90a0-b728123456789",
  "username": "admin",
  "email": "admin@example.com",
  "roles": ["ADMIN", "SUPER_ADMIN"],
  "role_priorities": 100,
  "iss": "User Management System",
  "iat": 1736249600,
  "exp": 1736253200
}
```

**Claims Explained:**

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | UUID | User ID (subject) |
| `username` | String | User's username |
| `email` | String | User's email address |
| `roles` | Array | List of role names assigned to user |
| `role_priorities` | Integer | Highest role priority (for authorization) |
| `iss` | String | Token issuer ("User Management System") |
| `iat` | Unix Timestamp | Issued at time |
| `exp` | Unix Timestamp | Expiration time |

### Refresh Token

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "666e8600-0234-5678-90a0-b728123456789",
  "type": "refresh",
  "iss": "User Management System",
  "iat": 1736249600,
  "exp": 1736854400
}
```

**Claims Explained:**

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | UUID | User ID (subject) |
| `type` | String | Always "refresh" to distinguish from access tokens |
| `iss` | String | Token issuer |
| `iat` | Unix Timestamp | Issued at time |
| `exp` | Unix Timestamp | Expiration time (7 days from issue) |

---

## Cookie Storage

### Setting Tokens (Login Response)

**Backend Code (AuthController.java):**
```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {

    LoginResponse response = authService.login(
        request.getUsernameOrEmail(),
        request.getPassword(),
        ipAddress,
        userAgent
    );

    // Access Token Cookie - 15 minutes
    ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", response.getAccessToken())
            .httpOnly(true)           // JavaScript cannot access
            .secure(false)            // Set true in production
            .sameSite("Lax")          // CSRF protection
            .path("/")
            .maxAge(Duration.ofMinutes(15))
            .build();

    // Refresh Token Cookie - 7 days
    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(7))
            .build();

    httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

    return ResponseEntity.ok(ApiResponse.success(new LoginResponse()));
}
```

### Actual HTTP Response Headers

```
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/; HttpOnly; SameSite=Lax; Max-Age=900
Set-Cookie: refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/; HttpOnly; SameSite=Lax; Max-Age=604800
```

### Clearing Tokens (Logout)

```java
private void clearTokens(HttpServletResponse response) {
    ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ZERO)  // Expire immediately
            .build();

    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ZERO)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
}
```

---

## Cookie Attributes

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `Name` | `accessToken` / `refreshToken` | Cookie identifier |
| `Value` | JWT string | The actual token |
| `HttpOnly` | `true` | **Security**: Prevents JavaScript access (XSS protection) |
| `Secure` | `false`* | **Security**: Only sent over HTTPS (enable in production) |
| `SameSite` | `Lax` | **Security**: Prevents CSRF attacks |
| `Path` | `/` | Sent to all API routes |
| `Max-Age` | 900 / 604800 | Access: 15min (900s), Refresh: 7days (604800s) |

### Security Details

#### HttpOnly
```
Prevents: document.cookie access via JavaScript
Benefit:  XSS attackers cannot steal tokens
Status:   Enabled (true) - Cannot be disabled
```

#### Secure
```
Prevents: Cookies sent over HTTP (unencrypted)
Benefit:  Tokens cannot be intercepted in transit
Status:   Currently false (development)
Action:   Set to true in production with HTTPS
```

#### SameSite
```
Values:
  - Strict:  Cookie only sent in first-party context
  - Lax:     Cookie sent in first-party + top-level navigations  (RECOMMENDED)
  - None:    Cookie sent in all contexts (requires Secure=true)

Benefit:  Prevents CSRF attacks from malicious sites
Status:   Lax (balanced security + usability)
```

---

## Token Generation

**File:** `JwtTokenProvider.java`

### Access Token Generation
```java
public String generateAccessToken(User user, List<Role> roles) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("username", user.getUsername())
        .claim("email", user.getEmail())
        .claim("roles", roles.stream().map(Role::getName).filter(Objects::nonNull).toList())
        .claim("role_priorities", roles.stream().mapToInt(Role::getPriority).max().orElse(0))
        .issuer(issuer)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)
        .compact();
}
```

### Refresh Token Generation
```java
public String generateRefreshToken(User user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);

    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("type", "refresh")
        .issuer(issuer)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)
        .compact();
}
```

---

## Token Validation

### Validate Token
```java
public Claims validateToken(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
}
```

### Check Token Type
```java
public boolean isRefreshToken(String token) {
    Claims claims = validateToken(token);
    return "refresh".equals(claims.get("type", String.class));
}

public boolean isAccessToken(String token) {
    Claims claims = validateToken(token);
    return !"refresh".equals(claims.get("type", String.class)) &&
           !"mfa_temp".equals(claims.get("type", String.class));
}
```

### Extract User ID
```java
public UUID getUserIdFromToken(String token) {
    Claims claims = validateToken(token);
    return UUID.fromString(claims.getSubject());
}
```

---

## Configuration

**File:** `application.yml` or `application.properties`

```yaml
jwt:
  secret: ums-secret-key-must-be-at-least-256-bits-long-for-hs256
  access-token-expiration: 900      # 15 minutes in seconds
  refresh-token-expiration: 604800   # 7 days in seconds
  issuer: User Management System
```

---

## Frontend Integration

### API Service (Reading Tokens from Cookies)

```typescript
// Get token from document.cookie
function getCookie(name: string): string | null {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
    return null;
}

// API calls automatically include cookies
const response = await fetch('/api/v1/users', {
    method: 'GET',
    credentials: 'include'  // Required to send cookies
});

// No manual token handling needed - cookies sent automatically
```

### AuthContext (Session Management)

```typescript
class AuthContext {
    private _accessToken: string | null = null;
    private _refreshToken: string | null = null;

    // Tokens read from cookies automatically on page load
    async initialize() {
        this._accessToken = getCookie('accessToken');
        this._refreshToken = getCookie('refreshToken');

        if (this._accessToken && !this.isTokenExpired(this._accessToken)) {
            this.setAuthenticated(true);
        } else if (this._refreshToken) {
            await this.refreshAccessToken();
        } else {
            this.setAuthenticated(false);
        }
    }

    // Logout - cookies cleared by server
    async logout() {
        await fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'include' });
        this.setAuthenticated(false);
    }
}
```

---

## Security Considerations

### What httpOnly Cookies Protect Against

| Attack Type | Description | Protection |
|-------------|-------------|------------|
| **XSS** | Malicious script tries to read `document.cookie` | Blocked - cookie not accessible to JavaScript |
| **Session Hijacking** | Attacker steals session token | Token hidden from JS |
| **Token Leakage** | Token logged to console | Token never exposed to client code |

### What httpOnly Cookies Do NOT Protect Against

| Attack Type | Description | Mitigation |
|-------------|-------------|------------|
| **CSRF** | Malicious site makes requests on behalf of user | SameSite=Lax header |
| **Network Sniffing** | Token intercepted in transit | Use HTTPS + Secure flag |
| **Physical Access** | Someone accesses user's device | Session timeout, device fingerprinting |
| **Malicious Browser Extension** | Extension with broad permissions | Content Security Policy |

### Production Recommendations

1. **Enable Secure Flag**
   ```java
   .secure(true)  // Only over HTTPS
   ```

2. **Consider Strict SameSite for Sensitive Operations**
   ```java
   .sameSite("Strict")  // Only first-party requests
   ```

3. **Shorten Access Token Lifetime**
   ```yaml
   jwt:
     access-token-expiration: 300  # 5 minutes
   ```

4. **Implement Token Rotation**
   - Refresh token should be rotated (new one issued on each use)
   - Old refresh tokens should be revoked

5. **Add Device Fingerprinting**
   - Bind tokens to device characteristics
   - Detect unusual login patterns

---

## Troubleshooting

### Common Issues

#### Token Not Sent with Requests
```
Problem: API returns 401 despite being logged in
Solution: Ensure credentials: 'include' is set in fetch options
```

#### Cookies Not Set After Login
```
Problem: Login succeeds but cookies missing
Solution: Check browser console for cookie warnings
         Ensure no SameSite policy conflicts
```

#### Cross-Origin Cookie Issues
```
Problem: Cookies not set when frontend and backend on different ports
Solution: Ensure SameSite=None and Secure=true
         Note: Requires HTTPS
```

### Testing Tokens

#### Decode JWT Online
1. Go to https://jwt.io
2. Paste token in "Encoded" field
3. See decoded header and payload

#### Check Browser Cookies
1. Open DevTools (F12)
2. Go to Application → Cookies
3. Select domain
4. View cookie values

#### Test API with cURL
```bash
# Include cookies in request
curl -v http://localhost:3000/api/v1/users \
  -H "Cookie: accessToken=<token>; refreshToken=<token>"
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Authenticate user, set cookies |
| POST | `/api/v1/auth/refresh` | Get new access token |
| POST | `/api/v1/auth/logout` | Clear cookies, revoke token |

---

## References

- [JWT.io](https://jwt.io) - JWT specification and debugging
- [OWASP Cookies](https://owasp.org/www-community/controls/SecureCookieAttribute) - Cookie security guidelines
- [SameSite Cookies](https://web.dev/samesite-cookies-explained/) - SameSite explanation
- [Spring ResponseCookie](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ResponseCookie.html) - Spring cookie API
