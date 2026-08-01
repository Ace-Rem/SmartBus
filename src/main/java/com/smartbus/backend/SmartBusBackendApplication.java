package com.smartbus.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartBusBackendApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(SmartBusBackendApplication.class, args);
    }

    /**
     * Loads key=value pairs from ./.env into system properties when not already set.
     * Keeps secrets out of committed YAML while supporting local/Docker runs.
     */
    private static void loadDotEnvIfPresent() {
        Path envFile = Paths.get(".env");
        if (!Files.isRegularFile(envFile)) {
            envFile = Paths.get("smartbus-backend", ".env");
        }
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Optional local helper; missing/unreadable .env is fine.
        }
    }
}
