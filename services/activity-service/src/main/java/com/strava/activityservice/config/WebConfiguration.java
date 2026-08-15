package com.strava.activityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves uploaded activity photos, ported from main's WebConfiguration.
 * Photo URLs are stored as /uploads/photos/<file>, so the handler maps that
 * prefix onto the configured upload directory (main hardcoded "file:uploads/";
 * deriving the location from app.upload.dir keeps the UPLOAD_DIR env override
 * working in containers).
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/photos}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/photos/**")
                .addResourceLocations(location);
    }
}
