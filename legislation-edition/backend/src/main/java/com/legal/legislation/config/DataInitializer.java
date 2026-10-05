package com.legal.legislation.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.SysUser;
import com.legal.legislation.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动期数据初始化器。
 *
 * <p>职责(立法版当前阶段):
 * <ul>
 *   <li>扫描 sys_user 表中 password_hash = 'INIT' 的种子用户,
 *       用 BCrypt(123456) 回填,确保登录走标准 BCrypt 校验路径</li>
 *   <li>打印一行"账号初始化摘要"到控制台,便于 Docker 部署时一眼确认</li>
 * </ul>
 *
 * <p>注意:任何失败必须 warn 而非抛异常,避免阻塞 backend 启动。
 * 数据库不可达时由 Spring 自动装配失败而非本类。
 */
@Slf4j
@Component
@Order(1)   // 早于任何业务 Runner
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String PLACEHOLDER      = "INIT";

    private final SysUserMapper   userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate     jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        ensureSeedPasswords();
    }

    /**
     * 找出 password_hash = 'INIT' 的种子用户,用 BCrypt 加密 DEFAULT_PASSWORD 覆盖。
     * 幂等:已加密过的用户(password_hash 以 $2a$ / $2b$ / $2y$ 开头)不会再次处理。
     */
    private void ensureSeedPasswords() {
        // ⚠️ sys_user 表没有 is_deleted 字段,但 application.yml 全局开了逻辑删除过滤
        // MyBatis Plus 的 selectList 会自动追加 "AND is_deleted = 0",导致表里查不到任何行
        // 这里用 JdbcTemplate 直查,绕开逻辑删除过滤
        List<Long> seedIds;
        try {
            seedIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_user WHERE password_hash = ?", Long.class, PLACEHOLDER);
        } catch (Exception ex) {
            log.warn("[DataInitializer] 扫描种子用户失败: {}", ex.getMessage());
            return;
        }
        if (seedIds.isEmpty()) {
            log.info("[DataInitializer] 种子用户密码已就绪,无需初始化 (INIT 占位 = 0)");
            return;
        }
        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int updated = jdbcTemplate.update(
            "UPDATE sys_user SET password_hash = ?, updated_at = ? WHERE password_hash = ?",
            hash, now, PLACEHOLDER);
        log.info("[DataInitializer] 已为 {} 个种子用户回填 BCrypt 密码 (默认密码: {}), 涉及账号 ids={}",
            updated, DEFAULT_PASSWORD, seedIds);
    }
}
