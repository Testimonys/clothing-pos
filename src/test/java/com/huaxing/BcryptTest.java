package com.huaxing;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        boolean matches = encoder.matches("admin123", hash);
        System.out.println("Hash matches admin123: " + matches);

        String newHash = encoder.encode("admin123");
        System.out.println("New hash for admin123: " + newHash);

        boolean matchesNew = encoder.matches("admin123", newHash);
        System.out.println("New hash matches: " + matchesNew);
    }
}
