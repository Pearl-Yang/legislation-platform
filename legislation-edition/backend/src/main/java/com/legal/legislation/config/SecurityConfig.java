package com.legal.legislation.config;

import com.legal.legislation.security.JwtAuthFilter;
import com.legal.legislation.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security 配置(JWT 接入版)。
 *
 * 鉴权策略:
 *   - permitAll: /auth/**(登录/刷新/健康)、OpenAPI/Knife4j 静态资源
 *   - authenticated: 其他 /api/** 业务接口(需带有效 JWT)
 *   - 角色控制:各业务接口上 @PreAuthorize("hasRole('ADMIN')") 自行决定
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})      // 由 WebMvcConfig 提供 CORS
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // === OpenAPI / Knife4j 静态资源(放行) ===
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/doc.html",
                    "/doc.html/**",
                    "/favicon.ico"
                ).permitAll()
                // === 鉴权相关(放行) ===
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET,  "/auth/health").permitAll()
                // === 健康检查 / Prometheus 指标(放行,容器探活 + Prometheus scrape) ===
                .requestMatchers(
                    "/actuator/health", "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                // === 爬虫 / 审计(限管理员) ===
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // === 业务接口(需登录) ===
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
