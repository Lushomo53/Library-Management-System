package com.library.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {
    private static final Map<String, String> env = new HashMap<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        env.put(parts[0].trim(), parts[1].trim());
                    });
        } catch (Exception e) {
            System.err.println(".env file not found or unreadable");
        }
    }

    public static String get(String key) {
        return env.get(key);
    }
}

