-- =====================================================================
-- V3: 注入 5 个角色种子用户(开发期演示用,密码默认 123456)
--
-- 背景:
--   - V1 只注入 admin (id=1) / user1 (id=2) 两个用户,IntegrationTest 默认 H2 内存数据库只看到这俩
--   - supplement_public.sql 注入了 5 个角色账号,但它不在 Flyway 迁移路径里,
--     docker compose 启动时不会自动跑
--   - D2 任务里"5 账号 admin / leader / drafter / reviewer / evaluator / 123456 全部可登录"
--     必须靠 V3 这条 Flyway 迁移自动跑通
--
-- 设计:
--   - 只 INSERT 4 个新角色(leader / drafter / reviewer / evaluator);
--     admin / user1 已在 V1 注入,V3 不重复(避免 username 唯一约束冲突)
--   - password_hash = 'INIT' 占位,启动后 DataInitializer 用 BCrypt(123456) 回填
--   - id 从 11 起,避开 V1 的 1/2 与后续 V4 seed-data 可能使用的 3-10
--   - role 字段统一用 ROLE_ 前缀(与 Spring Security 约定一致)
-- =====================================================================

-- 先删除旧重跑残留(幂等性,允许重复执行)
DELETE FROM sys_user WHERE username IN ('leader', 'drafter', 'reviewer', 'evaluator');

-- 注入 4 个新角色种子
INSERT INTO sys_user (id, username, display_name, password_hash, role, department, is_active)
VALUES
    (11, 'leader',    '王主任',     'INIT', 'ROLE_LEADER',     '法规处',     1),
    (12, 'drafter',   '李起草员',   'INIT', 'ROLE_USER',       '法规一处',   1),
    (13, 'reviewer',  '张审查员',   'INIT', 'ROLE_REVIEWER',   '法制科',     1),
    (14, 'evaluator', '赵评估员',   'INIT', 'ROLE_EVALUATOR',  '执法监督局', 1);