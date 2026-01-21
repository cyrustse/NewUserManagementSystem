package com.ums.service;

import com.ums.dto.auth.LoginRequest;
import com.ums.dto.auth.LoginResponse;
import com.ums.dto.auth.TokenResponse;
import com.ums.dto.auth.RefreshRequest;

public interface AuthService {
    
    LoginResponse login(String usernameOrEmail, String password, String ipAddress, String userAgent);
    
    TokenResponse refreshToken(RefreshRequest request);
    
    void logout(String token);
    
    boolean verifyMfa(String code, String tempToken);
}
