package com.smartbus.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordGeneratorRunner {

    @Bean
    CommandLineRunner generatePassword() {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            String password = "123456";

            String encoded = encoder.encode(password);

            System.out.println("==============================");
            System.out.println("Password: " + password);
            System.out.println("BCrypt  : " + encoded);
            System.out.println("==============================");
        };
    }
}