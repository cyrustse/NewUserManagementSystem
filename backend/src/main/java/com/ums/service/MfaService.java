package com.ums.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.*;
import dev.samstevens.totp.time.*;
import org.springframework.stereotype.Service;

@Service
public class MfaService {
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    public String generateQrCodeUrl(String secret, String issuer, String account) {
        return "otpauth://totp/" + issuer + ":" + account + "?secret=" + secret + "&issuer=" + issuer;
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
