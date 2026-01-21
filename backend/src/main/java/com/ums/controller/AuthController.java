package com.ums.controller;

import com.ums.dto.auth.*;
import com.ums.dto.common.ApiResponse;
import com.ums.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Duration ACCESS_TOKEN_MAX_AGE = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(7);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponse response = authService.login(
                request.getUsernameOrEmail(),
                request.getPassword(),
                ipAddress,
                userAgent
            );

            // If MFA is required, return mfaToken in body (not cookie)
            if (response.isRequiresMfa()) {
                // Clear any existing cookies
                clearTokens(httpResponse);
                // Return MFA token in body
                LoginResponse mfaResponse = new LoginResponse();
                mfaResponse.setRequiresMfa(true);
                mfaResponse.setMfaToken(response.getMfaToken());
                return ResponseEntity.ok(ApiResponse.success(mfaResponse));
            }

            // Set httpOnly cookies for tokens
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", response.getAccessToken())
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(ACCESS_TOKEN_MAX_AGE)
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(REFRESH_TOKEN_MAX_AGE)
                    .build();

            httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            // Return minimal response without tokens in body
            LoginResponse cookieResponse = new LoginResponse();
            cookieResponse.setRequiresMfa(false);
            return ResponseEntity.ok(ApiResponse.success(cookieResponse));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            // Get refresh token from cookie or request body
            String refreshToken = null;

            // Try to get from cookie first
            jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }

            // Fallback to request body
            if (refreshToken == null && request != null) {
                refreshToken = request.getRefreshToken();
            }

            if (refreshToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Refresh token is required"));
            }

            RefreshRequest refreshRequest = new RefreshRequest();
            refreshRequest.setRefreshToken(refreshToken);

            TokenResponse response = authService.refreshToken(refreshRequest);

            // Set new access token cookie
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", response.getAccessToken())
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(ACCESS_TOKEN_MAX_AGE)
                    .build();

            httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // Return minimal response
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken(response.getAccessToken());
            return ResponseEntity.ok(ApiResponse.success(tokenResponse));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        // Get refresh token from cookie to revoke it
        jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    authService.logout(cookie.getValue());
                    break;
                }
            }
        }

        // Clear all auth cookies
        clearTokens(httpResponse);

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private void clearTokens(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
