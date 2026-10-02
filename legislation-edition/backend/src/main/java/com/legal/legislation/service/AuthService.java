package com.legal.legislation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.common.exception.BizException;
import com.legal.legislation.entity.SysUser;
import com.legal.legislation.mapper.SysUserMapper;
import com.legal.legislation.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务。
 *
 * - 登录:校验密码 → 签发 JWT → 更新 last_login_at
 * - 刷新:用旧 token 换新 token(只要未过期)
 * - 当前用户:从 token 解析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getIsActive() != null && user.getIsActive() == 0) {
            throw BizException.forbidden("账号已被禁用");
        }
        // 兼容开发期:password_hash 为空时,任意密码放行(只控制台告警)
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            log.warn("[AuthService] 用户 {} 密码哈希为空,采用开发期直通登录", username);
        } else if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BizException.unauthorized("用户名或密码错误");
        }

        String token = jwtUtil.issue(user.getId(), user.getUsername(), user.getRole(), user.getDepartment());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return buildAuthPayload(user, token);
    }

    public Map<String, Object> refresh(String oldToken) {
        Claims claims = jwtUtil.parse(oldToken);
        if (claims == null) {
            throw BizException.unauthorized("token 无效或已过期");
        }
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw BizException.unauthorized("token 缺少 userId");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        String newToken = jwtUtil.issue(user.getId(), user.getUsername(), user.getRole(), user.getDepartment());
        return buildAuthPayload(user, newToken);
    }

    public Map<String, Object> me(String token) {
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            throw BizException.unauthorized("token 无效");
        }
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw BizException.unauthorized("token 缺少 userId");
        }
        SysUser user = userMapper.selectById(Long.valueOf(userIdObj.toString()));
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId",      user.getId());
        data.put("username",    user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("role",        user.getRole());
        data.put("department",  user.getDepartment());
        data.put("lastLoginAt", user.getLastLoginAt());
        return data;
    }

    private Map<String, Object> buildAuthPayload(SysUser user, String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("token",       token);
        data.put("tokenType",   "Bearer");
        data.put("expiresIn",   jwtUtil.getExpirationMs() / 1000);
        data.put("userId",      user.getId());
        data.put("username",    user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("role",        user.getRole());
        data.put("department",  user.getDepartment());
        return data;
    }
}
