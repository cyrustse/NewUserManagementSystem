package com.ums.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordServiceTest {
    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashPassword_ProducesValidHash() {
        String password = "testPassword123";
        String hash = passwordService.hashPassword(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertNotEquals("", hash);
        assertTrue(passwordService.verifyPassword(password, hash));
    }

    @Test
    void hashPassword_DifferentHashesForSamePassword() {
        String password = "testPassword123";
        String hash1 = passwordService.hashPassword(password);
        String hash2 = passwordService.hashPassword(password);

        assertNotEquals(hash1, hash2);
        assertTrue(passwordService.verifyPassword(password, hash1));
        assertTrue(passwordService.verifyPassword(password, hash2));
    }

    @Test
    void verifyPassword_WrongPassword_ReturnsFalse() {
        String password = "testPassword123";
        String hash = passwordService.hashPassword(password);

        assertFalse(passwordService.verifyPassword("wrongPassword", hash));
        assertFalse(passwordService.verifyPassword("", hash));
        assertFalse(passwordService.verifyPassword(null, hash));
    }

    @Test
    void verifyPassword_CorrectPassword_ReturnsTrue() {
        String password = "testPassword123!@#$%";
        String hash = passwordService.hashPassword(password);

        assertTrue(passwordService.verifyPassword(password, hash));
    }

    @Test
    void verifyPassword_EmptyPassword_ReturnsFalse() {
        String hash = passwordService.hashPassword("somePassword");

        assertFalse(passwordService.verifyPassword("", hash));
    }
}
