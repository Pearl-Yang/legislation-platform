package com.legal.legislation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类(JJWT 0.11.5 兼容版)。
 *
 * 算法:HS256(对称密钥)
 * Claims:sub=username, role, userId, dept, iat, exp
 *
 * 注意:
 *  - secret 从 application.yml 读入,生产环境务必通过环境变量覆盖;
 *  - 短 token(默认 24h);如需刷新机制由前端按 401 + /auth/refresh 处理。
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:legislationIntelligencePlatformSecretKey2026}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    private SecretKey key;
    private byte[] keyBytes;

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // HS256 要求 >= 256 位 = 32 字节
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            for (int i = bytes.length; i < 32; i++) {
                padded[i] = (byte) ('x' + (i % 5));
            }
            bytes = padded;
            log.warn("jwt.secret 长度不足 32 字节,已用占位填充,生产环境请覆盖");
        }
        this.keyBytes = bytes;
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** 签发 token。 */
    public String issue(Long userId, String username, String role, String department) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role",   role);
        claims.put("dept",   department);
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + expirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /** 解析 token,失败时返回 null。 */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parser()
                .setSigningKey(keyBytes)
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
