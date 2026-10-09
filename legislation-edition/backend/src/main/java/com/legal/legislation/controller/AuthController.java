package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.common.exception.BizException;
import com.legal.legislation.security.JwtAuthFilter;
import com.legal.legislation.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

/**
 * 认证 Controller。
 *
 * 接口:
 *   POST /auth/login    {username, password}         -> {token, userInfo, expiresIn}
 *   GET  /auth/me       Authorization: Bearer xxx    -> 当前用户
 *   POST /auth/refresh  {token}                      -> 新 token
 *   POST /auth/logout                                  客户端清 token 即可
 *   GET  /auth/health                                  健康检查
 */
@Tag(name = "公共-认证", description = "登录 / 刷新 / 当前用户")
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "用户名 + 密码登录,返回 JWT + 用户信息")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null) {
            throw BizException.badRequest("username 和 password 必填");
        }
        return Result.success("登录成功", authService.login(username, password));
    }

    @Operation(summary = "当前登录用户", description = "从 Authorization 头解析 token,返回当前用户")
    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestHeader(value = JwtAuthFilter.AUTH_HEADER, required = false) String header) {
        String token = extractToken(header);
        if (token == null) {
            throw BizException.unauthorized("未登录");
        }
        return Result.success(authService.me(token));
    }

    @Operation(summary = "刷新 token", description = "用旧 token 换新 token(token 未过期)")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw BizException.badRequest("token 必填");
        }
        return Result.success("刷新成功", authService.refresh(token));
    }

    @Operation(summary = "登出(客户端清 token 即可)", description = "无状态 JWT,服务端不保存会话")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已登出", null);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",  "UP");
        data.put("service", "legislation-edition-backend");
        data.put("version", "0.2.0");
        return Result.success(data);
    }

    private String extractToken(String header) {
        if (header == null) return null;
        if (header.startsWith(JwtAuthFilter.AUTH_PREFIX)) {
            return header.substring(JwtAuthFilter.AUTH_PREFIX.length()).trim();
        }
        return header.trim();
    }
}
