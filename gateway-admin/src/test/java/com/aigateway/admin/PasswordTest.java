package com.aigateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    @Test
    public void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "admin123456";
        String hash = encoder.encode(raw);
        System.out.println("\n===== BCrypt Hash =====");
        System.out.println("Password: " + raw);
        System.out.println("Hash: " + hash);
        System.out.println("Verify: " + encoder.matches(raw, hash));
        System.out.println("SQL: UPDATE admin_user SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("======================\n");
    }
}
