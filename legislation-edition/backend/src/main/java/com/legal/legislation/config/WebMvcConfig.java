package com.legal.legislation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置 - 默认放开所有来源(开发期友好)。
 *
 * 生产部署应改成具体前端域名,通过 legislation.cors.allowed-origins 配置。
 */
@Configuration
public class WebMvcConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Disposition")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
