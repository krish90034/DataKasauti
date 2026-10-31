 package com.datakasauti.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
            @Value("${datakasauti.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
            String configuredOrigins
    ) {
        List<String> origins = new ArrayList<>();

        if (configuredOrigins != null && !configuredOrigins.isBlank()) {
            String[] parts = configuredOrigins.split(",");

            for (String part : parts) {
                if (part == null) {
                    continue;
                }

                String trimmed = part.trim();

                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        // Keep safe development defaults if the configuration is blank.
        if (origins.isEmpty()) {
            origins.add("http://localhost:5173");
            origins.add("http://127.0.0.1:5173");
        }

        this.allowedOrigins = origins.toArray(new String[0]);
    }

    @SuppressWarnings("null")
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}