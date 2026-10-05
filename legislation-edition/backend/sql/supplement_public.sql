-- =====================================================================
-- 立法版 - 补建公共表(sys_user / notify_message)
--
-- 用途:之前跑 legislation_schema.sql 时,末尾 2 张表被截断,
--      这里补上建表 SQL。
--  幂等:DROP + CREATE + INSERT IF NOT EXISTS
--  执行:mysql -uroot -p123456 legal_legislation < supplement_public.sql
-- =====================================================================

SET NAMES utf8mb4;
USE `legal_legislation`;

-- ---------------------------------------------------------------------
-- 公共:通知消息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `notify_message`;
CREATE TABLE `notify_message` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `recipient_id`  BIGINT                                                COMMENT '接收人用户 ID(NULL = 系统广播)',
    `type`          VARCHAR(32)  NOT NULL                                COMMENT '消息类型:DEADLINE 期限 / CLEANUP 清理 / EVALUATION 评估 / SYSTEM 系统 / AUDIT 审计',
    `title`         VARCHAR(255) NOT NULL                                COMMENT '消息标题',
    `content`       TEXT                                                 COMMENT '消息正文',
    `biz_type`      VARCHAR(64)                                          COMMENT '关联业务实体类型:legislative_project / cleanup_task ...',
    `biz_id`        BIGINT                                                COMMENT '关联业务实体 ID',
    `channel`       VARCHAR(32)  NOT NULL DEFAULT 'CONSOLE'              COMMENT '发送通道:CONSOLE / INBOX / EMAIL / WEBSOCKET',
    `is_read`       TINYINT      NOT NULL DEFAULT 0                      COMMENT '是否已读:0 未读 / 1 已读',
    `sent_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipient` (`recipient_id`, `sent_at`),
    KEY `idx_type`      (`type`),
    KEY `idx_biz`       (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息落地表,后续消息中心读取此表';

-- ---------------------------------------------------------------------
-- 公共:系统用户(立法版最小登录态)
--  种子用户先插入 password_hash = 'INIT' 占位,
--  后端启动后由 DataInitializer 用 BCrypt 加密 123456 覆盖。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL                                COMMENT '登录用户名',
    `display_name`  VARCHAR(64)                                          COMMENT '显示名',
    `password_hash` VARCHAR(128) NOT NULL DEFAULT 'INIT'                 COMMENT 'BCrypt 密码哈希(对外不返回)',
    `role`          VARCHAR(32)  NOT NULL DEFAULT 'ROLE_USER'            COMMENT '角色:ROLE_USER / ROLE_ADMIN / ROLE_LEADER / ROLE_REVIEWER / ROLE_EVALUATOR',
    `department`    VARCHAR(128)                                          COMMENT '所属部门',
    `is_active`     TINYINT      NOT NULL DEFAULT 1                      COMMENT '是否启用:1 启用 / 0 禁用',
    `last_login_at` DATETIME                                              COMMENT '最后登录时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表:立法版最小登录态';

-- 种子用户 5 个不同角色,密码默认 123456
INSERT INTO `sys_user` (`username`, `display_name`, `role`, `department`) VALUES
('admin',     '系统管理员', 'ROLE_ADMIN',      '信息中心'),
('leader',    '王主任',     'ROLE_LEADER',     '法规处'),
('drafter',   '李起草员',   'ROLE_USER',       '法规一处'),
('reviewer',  '张审查员',   'ROLE_REVIEWER',   '法制科'),
('evaluator', '赵评估员',   'ROLE_EVALUATOR',  '执法监督局');

-- 验证
SELECT 'sys_user' AS table_name, COUNT(*) AS cnt FROM sys_user
UNION ALL SELECT 'notify_message', COUNT(*) FROM notify_message;
