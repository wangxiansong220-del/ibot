package com.bbu.ibot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://127.0.0.1:5500",
                        "http://localhost:5500",
                        "http://127.0.0.1:3000",
                        "http://localhost:3000",
                        "http://127.0.0.1:3001",
                        "http://localhost:3001",
                        "http://127.0.0.1:5173",
                        "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");

        registry.addMapping("/chat")
                .allowedOrigins(
                        "http://127.0.0.1:5500",
                        "http://localhost:5500",
                        "http://127.0.0.1:3000",
                        "http://localhost:3000",
                        "http://127.0.0.1:3001",
                        "http://localhost:3001",
                        "http://127.0.0.1:5173",
                        "http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
