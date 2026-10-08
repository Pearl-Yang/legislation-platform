-- =====================================================================
-- V2: 重置种子用户密码占位
--
-- 背景:
--   V1 早期把 admin / user1 的 password_hash 写死成了 BCrypt('$2a$10$NV0H8...')
--   但这个 hash 实际与 123456 不一致,导致 /auth/login 永远返回 40100。
--
-- 修复:
--   把种子用户 password_hash 改成 'INIT' 占位符,启动时由 DataInitializer
--   用 BCrypt(123456) 重新写入。dev / openapi / prod profile 一致生效。
--
-- 幂等: 已执行过的环境不会重复执行(Flyway 校验 checksum)
--
-- 安全提示: 生产环境若已为 admin 设过真实密码,请勿直接套用本 SQL,
--           应改用管理后台 / SQL 直接 UPDATE password_hash。
-- =====================================================================

UPDATE sys_user
   SET password_hash = 'INIT'
 WHERE id IN (1, 2)
   AND (password_hash LIKE '$2a$%'
        OR password_hash LIKE '$2b$%'
        OR password_hash LIKE '$2y$%');