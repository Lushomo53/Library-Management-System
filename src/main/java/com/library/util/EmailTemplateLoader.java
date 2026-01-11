package com.library.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class EmailTemplateLoader {

    private EmailTemplateLoader() {}

    public static String loadTemplate(String templateName) {
        String path = "/email/" + templateName;

        try (InputStream is = EmailTemplateLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Email template not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template", e);
        }
    }

    public static String render(String template, Map<String, String> values) {
        for (var entry : values.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }
}

