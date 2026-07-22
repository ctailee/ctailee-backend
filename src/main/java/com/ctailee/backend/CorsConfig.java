package com.ctailee.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // 哪些後端 API 路徑要套用這份 CORS 設定。/** 代表所有路徑。
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://127.0.0.1:5173",
                                "http://localhost:5173",
                                "https://ctailee.com"
                        )
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "OPTIONS"
                        )
                        .allowedHeaders("*"); // 前端請求可以帶哪些 request headers。* 代表全部
            }
        };
    }
}