package com.ums.dto.auth;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private boolean requiresMfa;
    private String mfaToken;
    
    public LoginResponse() {}
    
    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.requiresMfa = false;
    }
    
    public static LoginResponse withMfa(String mfaToken) {
        LoginResponse response = new LoginResponse();
        response.setRequiresMfa(true);
        response.setMfaToken(mfaToken);
        return response;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public boolean isRequiresMfa() {
        return requiresMfa;
    }
    
    public void setRequiresMfa(boolean requiresMfa) {
        this.requiresMfa = requiresMfa;
    }
    
    public String getMfaToken() {
        return mfaToken;
    }
    
    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }
}
