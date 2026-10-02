package com.legal.legislation.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.SysUser;
import com.legal.legislation.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器。
 *
 * 启动时:
 *  - 给 password_hash 为空的用户补上 BCrypt 哈希(开发期默认密码 123456)
 *  - 给 last_login_at 为空的用户填当前时间(便于前端显示)
 *
 * 生产环境:
 *  - 通过 SQL 注入或管理后台维护真实密码,本类不会再覆盖
 *  - 当 password_hash 已存在(非空)时,本类完全跳过,不会重置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    /** 开发期统一默认密码,生产请通过管理后台或 SQL 改写。 */
    public static final String DEV_DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            ensurePasswordHashes();
        } catch (Exception ex) {
            log.warn("[DataInitializer] 初始化失败,非致命,继续启动: {}", ex.getMessage());
        }
    }

    private void ensurePasswordHashes() {
        var users = userMapper.selectList(
            new QueryWrapper<SysUser>().isNull("password_hash").or().eq("password_hash", "")
        );
        if (users.isEmpty()) {
            log.info("[DataInitializer] 所有用户密码哈希已就位,无需初始化");
            return;
        }
        String hash = passwordEncoder.encode(DEV_DEFAULT_PASSWORD);
        int updated = 0;
        for (SysUser u : users) {
            u.setPasswordHash(hash);
            if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now());
            userMapper.updateById(u);
            updated++;
            log.info("[DataInitializer] 用户 {} ({}) 已初始化密码为 123456", u.getUsername(), u.getRole());
        }
        log.info("[DataInitializer] 共初始化 {} 个用户密码", updated);
    }
}
