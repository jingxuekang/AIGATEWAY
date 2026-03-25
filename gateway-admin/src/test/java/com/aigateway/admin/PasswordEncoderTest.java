package com.aigateway.admin;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderTest {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "134018";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("=".repeat(60));
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("=".repeat(60));
        System.out.println("\nSQL to update admin password:");
        System.out.println("UPDATE user SET password = '" + encodedPassword + "' WHERE username = 'admin';");
        System.out.println("=".repeat(60));
    }
}
