package com.ums.controller;

import com.ums.dto.common.ApiResponse;
import com.ums.service.MfaService;
import com.ums.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/mfa")
public class MfaController {
    @Autowired private MfaService mfaService;
    @Autowired private UserService userService;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<SetupResponse>> setupMfa() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String secret = mfaService.generateSecret();
        String qrUrl = mfaService.generateQrCodeUrl(secret, "UserManagementSystem", userId);
        return ResponseEntity.ok(ApiResponse.success(new SetupResponse(secret, qrUrl)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyMfa(@RequestBody VerifyRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Get user's MFA secret from database
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }

        var user = userOpt.get();
        String secret = user.getMfaSecret();

        if (secret == null || secret.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("MFA not setup"));
        }

        // Verify the code
        boolean isValid = mfaService.verifyCode(secret, request.code);

        if (isValid) {
            // Enable MFA for user
            userService.enableMfa(userId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid verification code"));
        }
    }

    public static class SetupResponse {
        public String secret;
        public String qrUrl;
        public SetupResponse(String secret, String qrUrl) {
            this.secret = secret;
            this.qrUrl = qrUrl;
        }
    }

    public static class VerifyRequest {
        public String code;
        public String secret;
    }
}
