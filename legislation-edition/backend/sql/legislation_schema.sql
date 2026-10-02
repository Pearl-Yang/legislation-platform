-- =====================================================================
-- 行政立法智能辅助平台 - 数据库初始化脚本
-- =====================================================================
-- 数据库：legal_legislation
-- 目标引擎：MySQL 8.0+
-- 字符集  ：utf8mb4 / utf8mb4_unicode_ci
--
-- 配套表数：24 张业务表
--   模块一 立法项目全流程    ：legislative_project / legislative_stage /
--                            legislative_deadline / legislative_stage_template
--   模块二 草案生成         ：legislative_draft / draft_version_history
--   模块三 智慧审查         ：review_record / review_issue / review_rule
--   模块四 智能清理         ：cleanup_task / cleanup_suggestion
--   模块五 实施评估         ：evaluation_task / evaluation_indicator /
--                            evaluation_result
--   模块六 意见征集         ：consultation / opinion / opinion_reply /
--                            opinion_category
--   模块七 立法资料库       ：library_material / library_tag / material_tag /
--                            material_note
--   共用 主表（贯穿模块四/三/七）: regulation / regulation_relation
--
-- 命名规范：
--   - 表名 snake_case，单数名词（如 legislative_project）
--   - 业务主键统一为 id BIGINT AUTO_INCREMENT
--   - 通用审计字段：created_at / updated_at / created_by / updated_by
--   - 软删除字段：is_deleted TINYINT（0=未删除，1=已删除）
--   - 业务状态字段：使用 VARCHAR 枚举值，COMMENT 中列出全部取值
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `legal_legislation` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `legal_legislation`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 模块一：行政立法项目全流程
--   业务说明：管理"立法项目"从立项、起草、审查、公布到备案的全生命周期。
--   表关系：1 项目 (legislative_project) → N 节点 (legislative_stage)
--                  → N 期限 (legislative_deadline)
--          节点定义由 legislative_stage_template 提供
-- =====================================================================

-- ---------------------------------------------------------------------
-- 立法项目主表
--   一条记录 = 一个立法项目（行政法规 / 部门规章 / 地方政府规章）
--   关联模块：模块二（草案）、模块三（审查）、模块六（意见征集）都会引用此表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `legislative_project`;
CREATE TABLE `legislative_project` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT                                       COMMENT '主键',
    `project_name`    VARCHAR(255) NOT NULL                                                      COMMENT '立法项目名称（如：xx市燃气管理条例）',
    `project_type`    VARCHAR(32)  NOT NULL                                                      COMMENT '项目类型：ADMIN_REGULATION 行政法规 / DEPT_RULE 部门规章 / LOCAL_RULE 地方政府规章',
    `description`     TEXT                                                                         COMMENT '项目说明：起草目的、适用范围、起草单位等',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT'                                      COMMENT '项目状态：DRAFT 草稿 / ACTIVE 进行中 / PUBLISHED 已发布 / OBSOLETE 已废止',
    `publish_date`    DATE                                                                          COMMENT '正式发布日期（status=PUBLISHED 后回填）',
    `created_by`      BIGINT                                                                        COMMENT '创建人用户 ID',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                              COMMENT '创建时间',
    `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    `is_deleted`      TINYINT     NOT NULL DEFAULT 0                                              COMMENT '软删除标记：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status`     (`status`)                                                                COMMENT '状态过滤索引',
    KEY `idx_type`       (`project_type`)                                                          COMMENT '类型过滤索引',
    KEY `idx_created_at` (`created_at`)                                                            COMMENT '按创建时间排序索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法项目主表：行政法规 / 部门规章 / 地方政府规章的全生命周期管理';

-- ---------------------------------------------------------------------
-- 立法项目流程节点表
--   由 LegislativeFlowService.initializeStages 在立项时根据模板实例化
--   一条记录 = 一个项目的一个流程节点（如：xx项目-起草阶段）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `legislative_stage`;
CREATE TABLE `legislative_stage` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `project_id`      BIGINT       NOT NULL                                COMMENT '所属立法项目 ID（→ legislative_project.id）',
    `stage_code`      VARCHAR(64)  NOT NULL                                COMMENT '节点编码：PROPOSAL 立项 / DRAFTING 起草 / CONSULTATION 征求意见 / REVIEW 审查 / DECISION 决定 / STATE_COUNCIL 国务院常务会议审议 / PUBLISH 公布 / FILE 备案 等',
    `stage_name`      VARCHAR(128) NOT NULL                                COMMENT '节点中文名（如：起草、征求意见）',
    `stage_order`     INT          NOT NULL                                COMMENT '节点顺序号：1,2,3... 同项目内按 stage_order 升序推进',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING'              COMMENT '节点状态：PENDING 待激活 / IN_PROGRESS 进行中 / DONE 已完成 / SKIPPED 已跳过',
    `operator_id`     BIGINT                                              COMMENT '最近操作人（节点完成 / 跳过时的用户 ID）',
    `operator_time`   DATETIME                                            COMMENT '最近操作时间',
    `remark`          TEXT                                                 COMMENT '节点操作备注（如：审查通过 / 已征求 30 个部门意见）',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`, `stage_order`)                        COMMENT '项目-阶段联合索引，用于查询某项目的所有节点',
    KEY `idx_status`  (`status`)                                          COMMENT '按状态过滤（如查找所有进行中节点）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法项目流程节点：每个立法项目由 7 个左右流程节点组成';

-- ---------------------------------------------------------------------
-- 立法项目期限节点表
--   每个节点对应一个期限（deadline）
--   由 DeadlineReminderTask 定时扫描并触发提醒
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `legislative_deadline`;
CREATE TABLE `legislative_deadline` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '主键',
    `project_id`          BIGINT       NOT NULL                            COMMENT '所属立法项目 ID（→ legislative_project.id）',
    `node_name`           VARCHAR(128) NOT NULL                            COMMENT '期限节点名称（与 legislative_stage.stage_name 对应）',
    `deadline_date`       DATE                                              COMMENT '到期日期：根据模板 default_days 自动计算',
    `remind_before_days`  INT          NOT NULL DEFAULT 7                  COMMENT '提前几天提醒，默认 7 天',
    `status`              VARCHAR(32)  NOT NULL DEFAULT 'PENDING'          COMMENT '期限状态：PENDING 待办 / DONE 已完成 / OVERDUE 已逾期',
    `reminded_at`         DATETIME                                          COMMENT '上次提醒时间，用于避免重复提醒',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`, `deadline_date`)                      COMMENT '项目-到期日联合索引',
    KEY `idx_status`  (`status`)                                          COMMENT '按状态过滤（找逾期 / 待办）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法项目期限节点：用于定时任务发起的到期预警';

-- ---------------------------------------------------------------------
-- 立法流程模板配置表
--   把《行政法规制定程序条例》《规章制定程序条例》做成配置数据
--   LegislativeFlowService 立项时按 project_type 加载本表实例化节点
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `legislative_stage_template`;
CREATE TABLE `legislative_stage_template` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `type`          VARCHAR(32)  NOT NULL                                COMMENT '适用项目类型：ADMIN_REGULATION / DEPT_RULE / LOCAL_RULE',
    `stage_code`    VARCHAR(64)  NOT NULL                                COMMENT '节点编码（同立项时一样在 stage.stage_code 中使用）',
    `stage_name`    VARCHAR(128) NOT NULL                                COMMENT '节点中文名',
    `stage_order`   INT          NOT NULL                                COMMENT '节点顺序号',
    `default_days`  INT          NOT NULL DEFAULT 30                      COMMENT '该节点默认持续天数（用于计算 deadline）',
    `is_required`   TINYINT      NOT NULL DEFAULT 1                      COMMENT '是否必须经过：1 必须 / 0 可跳过',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_order` (`type`, `stage_order`)                   COMMENT '同一类型下顺序号唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法流程模板配置：定义行政法规/部门规章/地方政府规章三种流程';

-- =====================================================================
-- 模块二：草案生成
--   业务说明：AI 生成立法草案，每次生成产生一条新记录，并写入版本历史
-- =====================================================================

-- ---------------------------------------------------------------------
-- 立法草案主表
--   一条记录 = 一个立法项目的一份草案
--   同一 project 下的多份 draft 之间通过 version.diff 来区分
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `legislative_draft`;
CREATE TABLE `legislative_draft` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `project_id`            BIGINT       NOT NULL                                COMMENT '所属立法项目 ID（→ legislative_project.id）',
    `stage_id`              BIGINT                                                COMMENT '所属阶段 ID（→ legislative_stage.id），可空',
    `draft_content`         MEDIUMTEXT                                            COMMENT '草案正文（Markdown / 富文本）',
    `source_regulation_id`  BIGINT                                                COMMENT '参考的上位法 ID（→ regulation.id，可空）',
    `referenced_texts`      JSON                                                 COMMENT '引用的异地规章片段：[{regulationId, excerpt}] JSON 数组',
    `version`               INT          NOT NULL DEFAULT 1                      COMMENT '版本号：同 project 内从 1 开始递增',
    `generation_type`       VARCHAR(32)  NOT NULL DEFAULT 'AUTO_GENERATED'        COMMENT '生成方式：AUTO_GENERATED AI 自动生成 / MANUAL 人工起草',
    `prompt_snapshot`       MEDIUMTEXT                                            COMMENT '生成时的提示词快照，便于回溯与复核',
    `created_by`            BIGINT                                                COMMENT '创建人',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)                                              COMMENT '项目查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法草案主表：每次生成产生一条记录，版本迭代通过 version 控制';

-- ---------------------------------------------------------------------
-- 立法草案版本历史表
--   草案被修改时，把旧版本写入此表以追溯历史
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `draft_version_history`;
CREATE TABLE `draft_version_history` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `draft_id`          BIGINT       NOT NULL                                COMMENT '所属草案 ID（→ legislative_draft.id）',
    `version`           INT          NOT NULL                                COMMENT '历史版本号',
    `content_snapshot`  MEDIUMTEXT                                            COMMENT '该版本的完整正文快照',
    `changed_by`        BIGINT                                                COMMENT '修改人',
    `changed_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '修改时间',
    `change_summary`    VARCHAR(512)                                          COMMENT '本次变更摘要（如：第 12 条增加罚款额度）',
    PRIMARY KEY (`id`),
    KEY `idx_draft` (`draft_id`, `version`)                                    COMMENT '草案-版本号联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法草案版本历史：每次修改都留下快照，方便 diff 与责任追溯';

-- =====================================================================
-- 模块三：智慧审查
--   业务说明：基于规则的自动审查 + AI 增强审查，输出"红/黄/蓝/灰"四级问题
-- =====================================================================

-- ---------------------------------------------------------------------
-- 审查记录表
--   一次审查 = 一条记录，包含审查结论与总体通过情况
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `review_record`;
CREATE TABLE `review_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `draft_id`       BIGINT       NOT NULL                                COMMENT '被审查的草案 ID（→ legislative_draft.id）',
    `review_type`    VARCHAR(32)  NOT NULL DEFAULT 'AUTO'                  COMMENT '审查类型：AUTO 自动审查 / MANUAL 人工审查',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING'              COMMENT '审查状态：PENDING 待执行 / DONE 已完成',
    `reviewed_by`    BIGINT                                                COMMENT '执行审查的用户 ID（AUTO 时为空）',
    `reviewed_at`    DATETIME                                              COMMENT '审查完成时间',
    `overall_pass`   TINYINT                                                COMMENT '总体是否通过：1 通过 / 0 不通过 / NULL 待定',
    `error_count`    INT          NOT NULL DEFAULT 0                      COMMENT '本次审查发现的问题总数（汇总 review_issue）',
    PRIMARY KEY (`id`),
    KEY `idx_draft`  (`draft_id`)                                          COMMENT '按草案查询审查记录',
    KEY `idx_status` (`status`)                                            COMMENT '按状态过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查记录表：每次对一份草案执行审查产生一条记录';

-- ---------------------------------------------------------------------
-- 审查问题明细表
--   一条记录 = 一次审查发现的一个问题
--   severity 决定在审查报告中以何种颜色高亮
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `review_issue`;
CREATE TABLE `review_issue` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `review_record_id`         BIGINT       NOT NULL                                COMMENT '所属审查记录 ID（→ review_record.id）',
    `issue_type`               VARCHAR(64)  NOT NULL                                COMMENT '问题类型：SUPERIOR_CONFLICT 与上位法冲突 / OVER_POWER 越权 / OUTDATED_REF 失效引用 / DUPLICATE 重复 / FORMAT 格式 / VERBOSE 冗长',
    `severity`                 VARCHAR(16)  NOT NULL                                COMMENT '严重级别：RED 必须修改 / YELLOW 建议修改 / BLUE 提示 / GREY AI 建议',
    `article_index`            VARCHAR(64)                                          COMMENT '问题出现的条款编号（如：第十二条）',
    `description`              TEXT                                                 COMMENT '问题详情描述',
    `suggestion`                TEXT                                                 COMMENT 'AI / 规则给出的修改建议',
    `reference_regulation_id`  BIGINT                                                COMMENT '关联的上位法规 ID（→ regulation.id，可空）',
    `is_resolved`              TINYINT      NOT NULL DEFAULT 0                      COMMENT '是否已解决：0 未解决 / 1 已解决',
    PRIMARY KEY (`id`),
    KEY `idx_record`  (`review_record_id`)                                          COMMENT '按审查记录查询问题',
    KEY `idx_severity`(`severity`)                                                  COMMENT '按严重级别过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查问题明细：每次审查发现的单个问题，与 review_record 是 1:N';

-- ---------------------------------------------------------------------
-- 审查规则配置表
--   规则可启用 / 禁用；check_logic 使用 JSON 表达式配置规则条件
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `review_rule`;
CREATE TABLE `review_rule` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `rule_code`   VARCHAR(64)  NOT NULL                                COMMENT '规则唯一编码（如：SUPERIOR_CONFLICT）',
    `rule_name`   VARCHAR(128) NOT NULL                                COMMENT '规则中文名',
    `rule_type`   VARCHAR(64)  NOT NULL                                COMMENT '规则类型：LEGAL 合法性 / FORMAT 格式 / AI 智能',
    `check_logic` JSON                                                 COMMENT '规则检查逻辑 JSON 表达式：{"op":"...","params":"..."}',
    `severity`    VARCHAR(16)  NOT NULL DEFAULT 'YELLOW'              COMMENT '规则默认严重级别：RED / YELLOW / BLUE / GREY',
    `enabled`     TINYINT      NOT NULL DEFAULT 1                      COMMENT '是否启用：1 启用 / 0 禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`)                             COMMENT '规则编码唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查规则配置表：定义所有可执行的审查规则，规则引擎按本表逐条评估';

-- =====================================================================
-- 共用：法规主表（模块三审查、模块四清理、模块七资料库 共享）
--   业务说明：所有"立法资料库"的法规类条目，都先入此表
-- =====================================================================

-- ---------------------------------------------------------------------
-- 法规主表
--   一条记录 = 一份"行政法规 / 部门规章 / 地方政府规章"
--   regulation_type + status 决定它的法律效力
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `regulation`;
CREATE TABLE `regulation` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `regulation_name`     VARCHAR(255) NOT NULL                                COMMENT '法规名称（如：xx市燃气管理条例）',
    `regulation_type`     VARCHAR(32)  NOT NULL                                COMMENT '法规类型：ADMIN_REGULATION 行政法规 / DEPT_RULE 部门规章 / LOCAL_RULE 地方政府规章',
    `issuing_authority`   VARCHAR(128)                                          COMMENT '制定机关（国务院 / 部委 / 地方政府）',
    `issue_number`        VARCHAR(128)                                          COMMENT '发文字号（如：国务院令第 xxx 号）',
    `issue_date`          DATE                                                  COMMENT '发布 / 公布日期',
    `effective_date`      DATE                                                  COMMENT '生效日期',
    `expire_date`         DATE                                                  COMMENT '失效日期（被废止时填）',
    `status`              VARCHAR(32)  NOT NULL DEFAULT 'EFFECTIVE'            COMMENT '法规状态：EFFECTIVE 现行有效 / REVISING 修订中 / OBSOLETE 已废止',
    `full_text`           MEDIUMTEXT                                            COMMENT '法规全文',
    `digest`              TEXT                                                 COMMENT '摘要（用于列表与全文搜索对照）',
    `source_url`          VARCHAR(512)                                          COMMENT '原文链接',
    `region_code`         VARCHAR(16)                                          COMMENT '行政区划代码（如 110000 北京 / 320000 江苏）；全国性法规填 000000',
    `is_from_crawler`     TINYINT      NOT NULL DEFAULT 0                      COMMENT '是否由 crawler 抓取：1 是 / 0 否',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type`    (`regulation_type`)                                      COMMENT '按类型过滤',
    KEY `idx_status`  (`status`)                                              COMMENT '按状态过滤',
    KEY `idx_region`  (`region_code`)                                          COMMENT '按地区过滤',
    FULLTEXT KEY `ft_regulation_text` (`regulation_name`, `digest`, `full_text`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规主表：行政法规 / 部门规章 / 地方政府规章全文，是清理、审查、资料库的共用核心';

-- ---------------------------------------------------------------------
-- 法规上下位 / 引用关系表
--   一条记录 = 两个法规之间的一条关系
--   同时写本表和 Neo4j（用于图谱可视化）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `regulation_relation`;
CREATE TABLE `regulation_relation` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `source_id`         BIGINT       NOT NULL                                COMMENT '源法规 ID（→ regulation.id）',
    `target_id`         BIGINT       NOT NULL                                COMMENT '目标法规 ID（→ regulation.id）',
    `relation_type`     VARCHAR(32)  NOT NULL                                COMMENT '关系类型：SUPERIOR 上位-下位 / REFERENCE 引用 / SUBSTITUTE 替代 / OBSOLETE 废止',
    `related_article`   VARCHAR(64)                                          COMMENT '关联条款编号（如：第十二条第三款）',
    `description`       VARCHAR(512)                                          COMMENT '关系描述',
    `confidence_score`  DECIMAL(5,4) DEFAULT 1.0000                          COMMENT '置信度（AI 抽取时使用，1.0000 = 人工确认）',
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source_id`)                                            COMMENT '源端索引（找下位法规）',
    KEY `idx_target` (`target_id`)                                            COMMENT '目标端索引（找上位法规）',
    KEY `idx_type`   (`relation_type`)                                        COMMENT '按关系类型过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规上下位 / 引用 / 替代 / 废止关系表，与 Neo4j 图谱同步';

-- =====================================================================
-- 模块四：智能清理
--   业务说明：依据上位法变更或定期主题，对存量法规做清理
--   cleanup_task 是一次清理任务，cleanup_suggestion 是每条法规的处置建议
-- =====================================================================

-- ---------------------------------------------------------------------
-- 清理任务表
--   一条记录 = 一次清理任务
--   任务触发后批量生成 cleanup_suggestion
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cleanup_task`;
CREATE TABLE `cleanup_task` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `task_name`              VARCHAR(255) NOT NULL                                COMMENT '任务名称（如：xx上位法变更触发的关联清理）',
    `task_type`              VARCHAR(32)  NOT NULL                                COMMENT '任务类型：DAILY 日常清理 / PERIODIC 定期清理 / THEMATIC 专项主题清理',
    `status`                 VARCHAR(32)  NOT NULL DEFAULT 'PENDING'              COMMENT '任务状态：PENDING 待执行 / RUNNING 进行中 / DONE 已完成',
    `trigger_regulation_id`  BIGINT                                                COMMENT '触发清理的上位法 ID（→ regulation.id），DAILY 类型置空',
    `theme`                  VARCHAR(255)                                          COMMENT '专项主题关键词（仅 THEMATIC 类型有值，如：营商环境 / 证照分离）',
    `cleanup_mode`           VARCHAR(32)                                          COMMENT '清理模式：AUTO 全自动 / ASSISTED 人机协同',
    `created_by`             BIGINT                                                COMMENT '任务创建人',
    `created_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `completed_at`           DATETIME                                              COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_status`  (`status`)                                                COMMENT '按状态过滤',
    KEY `idx_trigger` (`trigger_regulation_id`)                                  COMMENT '按触发源索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清理任务表：日常 / 定期 / 专项 三种清理任务的统一管理';

-- ---------------------------------------------------------------------
-- 清理建议表
--   一条记录 = 一次清理任务中，对一份法规给出的处置建议
--   final_decision 字段：人工复核后可改写
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cleanup_suggestion`;
CREATE TABLE `cleanup_suggestion` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `task_id`         BIGINT       NOT NULL                                COMMENT '所属清理任务 ID（→ cleanup_task.id）',
    `regulation_id`   BIGINT       NOT NULL                                COMMENT '被建议处置的法规 ID（→ regulation.id）',
    `suggestion`      VARCHAR(32)  NOT NULL                                COMMENT 'AI 建议：KEEP 保留 / MODIFY 修改 / OBSOLETE 废止',
    `reason`          VARCHAR(512)                                          COMMENT 'AI 给出建议的理由',
    `ai_confidence`   DECIMAL(5,4)                                          COMMENT 'AI 置信度（0~1）',
    `final_decision`  VARCHAR(32)                                          COMMENT '人工最终决定：APPROVED 通过 / REJECTED 驳回 / PENDING 待定',
    `decided_by`      BIGINT                                                COMMENT '最终决策人',
    `decided_at`      DATETIME                                              COMMENT '最终决策时间',
    PRIMARY KEY (`id`),
    KEY `idx_task`       (`task_id`)                                          COMMENT '按任务查询建议',
    KEY `idx_regulation` (`regulation_id`)                                    COMMENT '按法规查询建议历史'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清理建议表：AI + 人工复核，给出每份法规的处置建议';

-- =====================================================================
-- 模块五：实施评估
--   业务说明：对已发布的法规，定期评估其实施效果
--   evaluation_indicator 定义评估维度与指标
--   evaluation_task 是一次评估任务
--   evaluation_result 是每个指标的具体得分
-- =====================================================================

-- ---------------------------------------------------------------------
-- 评估任务表
--   一条记录 = 对某部法规在某评估周期的评估任务
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation_task`;
CREATE TABLE `evaluation_task` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `regulation_id`   BIGINT       NOT NULL                                COMMENT '被评估的法规 ID（→ regulation.id）',
    `period_start`    DATE                                                  COMMENT '评估周期起始日',
    `period_end`      DATE                                                  COMMENT '评估周期结束日',
    `overall_score`   DECIMAL(6,3)                                          COMMENT '综合得分（0~100）',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING'              COMMENT '任务状态：PENDING 待执行 / RUNNING 进行中 / COMPLETED 已完成',
    `report_content`  MEDIUMTEXT                                            COMMENT '评估报告内容（HTML / Markdown）',
    `created_by`      BIGINT                                                COMMENT '任务创建人',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `completed_at`    DATETIME                                              COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_regulation` (`regulation_id`)                                    COMMENT '按法规查询评估历史',
    KEY `idx_status`     (`status`)                                          COMMENT '按状态过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估任务表：对一份法规在某评估周期内执行评估';

-- ---------------------------------------------------------------------
-- 评估指标配置表
--   一条记录 = 一个可重复使用的评估指标（如：行政相对人执行率）
--   weight 用于综合得分加权
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation_indicator`;
CREATE TABLE `evaluation_indicator` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `indicator_name` VARCHAR(128) NOT NULL                                COMMENT '指标中文名（如：行政相对人执行率）',
    `dimension`      VARCHAR(32)  NOT NULL                                COMMENT '所属维度：LEGALITY 合法性 / EXECUTION 落实性 / SATISFACTION 满意度',
    `weight`         DECIMAL(5,4) NOT NULL DEFAULT 0.1000                COMMENT '权重（0~1），所有指标 weight 之和应 = 1.0000',
    `formula`        VARCHAR(512)                                          COMMENT '计算公式表达式',
    `data_source`    VARCHAR(128)                                          COMMENT '数据来源（来自哪张表 / 哪个外部系统）',
    `unit`           VARCHAR(32)  NOT NULL DEFAULT 'RATIO'                COMMENT '单位：RATIO 比率 / ABSOLUTE 绝对值 / SCORE 分数',
    `enabled`        TINYINT      NOT NULL DEFAULT 1                      COMMENT '是否启用：1 启用 / 0 禁用',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dimension` (`dimension`)                                        COMMENT '按维度过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估指标配置表：定义三大维度下的具体指标及其权重';

-- ---------------------------------------------------------------------
-- 评估结果明细表
--   一条记录 = 一次评估任务中某个指标的得分
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation_result`;
CREATE TABLE `evaluation_result` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `task_id`           BIGINT       NOT NULL                                COMMENT '所属评估任务 ID（→ evaluation_task.id）',
    `indicator_id`      BIGINT       NOT NULL                                COMMENT '指标 ID（→ evaluation_indicator.id）',
    `raw_value`         DECIMAL(12,4)                                        COMMENT '原始数据值（未经归一化）',
    `normalized_score`  DECIMAL(6,3)                                          COMMENT '归一化后得分（0~100）',
    `weight`            DECIMAL(5,4)                                          COMMENT '当期权重（可与指标 weight 不同）',
    `dimension_score`   DECIMAL(6,3)                                          COMMENT '所属维度的小计分（同一 dimension 内所有指标的加权平均）',
    `rank`              VARCHAR(32)                                          COMMENT '在同类法规中的排名（如：12/200）',
    `data_snapshot`     JSON                                                 COMMENT '原始数据快照，JSON 结构，便于审计',
    `period_label`      VARCHAR(32)                                          COMMENT '评估周期标签（如：2024Q4 / 2024H1）',
    PRIMARY KEY (`id`),
    KEY `idx_task`      (`task_id`)                                          COMMENT '按任务查询所有指标结果',
    KEY `idx_indicator` (`indicator_id`)                                    COMMENT '按指标查询历史结果'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估结果明细：每个指标在一次评估任务中的得分';

-- =====================================================================
-- 模块六：意见征集
--   业务说明：对某次征集发布公告，公众提交意见，平台做去重与归类
--   consultation 是公告，opinion 是每条意见，opinion_reply 是回复
--   opinion_category 是按主题自动归类
-- =====================================================================

-- ---------------------------------------------------------------------
-- 意见征集公告表
--   一条记录 = 一次意见征集活动
--   可关联到立法项目 / 草案 / 清理任务（任意一个）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `consultation`;
CREATE TABLE `consultation` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `title`                    VARCHAR(255) NOT NULL                                COMMENT '征集标题（如：xx草案公开征求意见）',
    `description`              TEXT                                                 COMMENT '征集说明：背景、范围、参与方式',
    `related_project_id`       BIGINT                                                COMMENT '关联立法项目 ID（→ legislative_project.id，可空）',
    `related_draft_id`         BIGINT                                                COMMENT '关联草案 ID（→ legislative_draft.id，可空）',
    `related_cleanup_task_id`  BIGINT                                                COMMENT '关联清理任务 ID（→ cleanup_task.id，可空）',
    `status`                   VARCHAR(32)  NOT NULL DEFAULT 'DRAFT'                  COMMENT '征集状态：DRAFT 草稿 / OPEN 征集中 / CLOSED 已结束',
    `start_date`               DATE                                                  COMMENT '征集开始日期',
    `end_date`                 DATE                                                  COMMENT '征集截止日期',
    `total_views`              INT          NOT NULL DEFAULT 0                      COMMENT '累计浏览量',
    `total_opinions`           INT          NOT NULL DEFAULT 0                      COMMENT '累计意见数（汇总 opinion 表）',
    `created_by`               BIGINT                                                COMMENT '公告创建人',
    `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_status`     (`status`)                                              COMMENT '按状态过滤（找开放中的征集）',
    KEY `idx_start_date` (`start_date`)                                          COMMENT '按开始时间排序',
    KEY `idx_end_date`   (`end_date`)                                            COMMENT '按截止时间排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见征集公告表：每次征集活动产生一条记录';

-- ---------------------------------------------------------------------
-- 公众意见表
--   一条记录 = 一条公众意见
--   similarity_hash 用于 SimHash / Embedding 去重
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `opinion`;
CREATE TABLE `opinion` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `consultation_id`          BIGINT       NOT NULL                                COMMENT '所属征集 ID（→ consultation.id）',
    `parent_id`                BIGINT                                                COMMENT '父意见 ID（用于回复上级意见的引用，可空）',
    `submitter_name`           VARCHAR(64)                                          COMMENT '提交者姓名（可匿名提交）',
    `submitter_contact`        VARCHAR(64)                                          COMMENT '提交者联系方式（邮箱 / 手机）',
    `content`                  TEXT         NOT NULL                                COMMENT '意见正文',
    `attachment_urls`          JSON                                                 COMMENT '附件 URL 列表，JSON 数组',
    `similarity_hash`           VARCHAR(64)                                          COMMENT 'SimHash / embedding 哈希，用于去重',
    `status`                   VARCHAR(32)  NOT NULL DEFAULT 'NEW'                   COMMENT '处理状态：NEW 新提交 / PROCESSED 已处理 / REPLIED 已回复',
    `submitted_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '提交时间',
    `classified_category`      VARCHAR(64)                                          COMMENT 'AI 自动归类的类别（如：扩大适用范围）',
    `ai_category_confidence`   DECIMAL(5,4)                                          COMMENT 'AI 归类的置信度',
    `processed_by`             BIGINT                                                COMMENT '处理人',
    `processed_at`             DATETIME                                              COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_consult` (`consultation_id`)                                          COMMENT '按征集查询所有意见',
    KEY `idx_status`  (`status`)                                                  COMMENT '按状态过滤（如找未回复的）',
    KEY `idx_hash`    (`similarity_hash`)                                          COMMENT '相似度哈希索引，用于去重查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众意见表：每条意见一条记录，自动归类与去重';

-- ---------------------------------------------------------------------
-- 意见回复表
--   一条记录 = 一条意见的一次官方回复
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `opinion_reply`;
CREATE TABLE `opinion_reply` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `opinion_id`    BIGINT       NOT NULL                                COMMENT '所属意见 ID（→ opinion.id）',
    `reply_content` TEXT                                                 COMMENT '回复内容',
    `reply_by`      BIGINT                                                COMMENT '回复人',
    `reply_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '回复时间',
    `notify_sent`   TINYINT      NOT NULL DEFAULT 0                      COMMENT '是否已通知提交者：1 是 / 0 否',
    PRIMARY KEY (`id`),
    KEY `idx_opinion` (`opinion_id`)                                      COMMENT '按意见查询所有回复'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见回复表：官方对每条意见的回复';

-- ---------------------------------------------------------------------
-- 意见分类表
--   每次征集自动按内容归纳为若干类别（人工 / AI 均可）
--   用于在征集报告中按类别展示意见分布
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `opinion_category`;
CREATE TABLE `opinion_category` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `consultation_id` BIGINT       NOT NULL                                COMMENT '所属征集 ID（→ consultation.id）',
    `category_name`  VARCHAR(128) NOT NULL                                COMMENT '类别名称（如：扩大适用范围 / 提高处罚力度）',
    `opinion_count`  INT          NOT NULL DEFAULT 0                      COMMENT '本类别下的意见总数',
    `support_count`  INT          NOT NULL DEFAULT 0                      COMMENT '支持性意见数',
    `oppose_count`   INT          NOT NULL DEFAULT 0                      COMMENT '反对意见数',
    `neutral_count`  INT          NOT NULL DEFAULT 0                      COMMENT '中性 / 建议性意见数',
    PRIMARY KEY (`id`),
    KEY `idx_consult` (`consultation_id`)                                  COMMENT '按征集查询所有类别'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见分类表：每期征集自动 / 手工归纳的意见类别及统计';

-- =====================================================================
-- 模块七：立法资料库
--   业务说明：立法工作者的"资料库"
--   不限于"法规"，还包含草案、评估报告、专家意见、典型案例
-- =====================================================================

-- ---------------------------------------------------------------------
-- 立法资料条目表
--   一条记录 = 一份资料
--   material_type 决定存储内容类型与来源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `library_material`;
CREATE TABLE `library_material` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `title`             VARCHAR(255) NOT NULL                                COMMENT '资料标题',
    `material_type`     VARCHAR(32)  NOT NULL                                COMMENT '资料类型：REGULATION 法规 / DRAFT 草案 / REPORT 评估报告 / EXPERT_OPINION 专家意见 / CASE 典型案例',
    `region_code`       VARCHAR(16)                                          COMMENT '行政区划代码（资料归属地区）',
    `issuing_authority` VARCHAR(128)                                          COMMENT '发布机构',
    `issue_date`        DATE                                                  COMMENT '发布日期',
    `effective_date`    DATE                                                  COMMENT '生效日期（仅 REGULATION 类型有值）',
    `keywords`          JSON                                                 COMMENT '关键词数组，JSON',
    `file_url`          VARCHAR(512)                                          COMMENT '原始文件 URL（PDF / Word）',
    `digest`            TEXT                                                 COMMENT '摘要（用于检索结果展示）',
    `full_text`         MEDIUMTEXT                                            COMMENT '全文内容（用于全文检索）',
    `reference_count`   INT          NOT NULL DEFAULT 0                      COMMENT '被引用次数（被立法项目 / 草案引用的次数）',
    `view_count`        INT          NOT NULL DEFAULT 0                      COMMENT '浏览次数',
    `created_by`        BIGINT                                                COMMENT '录入人',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type`    (`material_type`)                                      COMMENT '按类型过滤',
    KEY `idx_region`  (`region_code`)                                        COMMENT '按地区过滤',
    KEY `idx_issue`   (`issue_date`)                                         COMMENT '按发布日期排序',
    FULLTEXT KEY `ft_full_text` (`title`, `digest`, `full_text`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法资料库条目：法规 / 草案 / 评估报告 / 专家意见 / 典型案例的统一管理';

-- ---------------------------------------------------------------------
-- 立法资料标签表
--   三种标签类型：领域（DOMAIN）/ 层级（LEVEL）/ 地区（REGION）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `library_tag`;
CREATE TABLE `library_tag` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `tag_name`   VARCHAR(64)  NOT NULL                                COMMENT '标签名称',
    `tag_type`   VARCHAR(32)  NOT NULL                                COMMENT '标签类型：DOMAIN 领域 / LEVEL 层级 / REGION 地区',
    `usage_count` INT         NOT NULL DEFAULT 0                      COMMENT '被使用的次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag` (`tag_name`, `tag_type`)                       COMMENT '同一类型下标签名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立法资料标签：用于多维分类';

-- ---------------------------------------------------------------------
-- 资料-标签关联表
--   一条记录 = 一份资料的一个标签（多对多关系）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `material_tag`;
CREATE TABLE `material_tag` (
    `material_id` BIGINT NOT NULL                                        COMMENT '资料 ID（→ library_material.id）',
    `tag_id`      BIGINT NOT NULL                                        COMMENT '标签 ID（→ library_tag.id）',
    PRIMARY KEY (`material_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料-标签关联表：多对多关系';

-- ---------------------------------------------------------------------
-- 资料批注表
--   一条记录 = 用户对某段资料的一次批注
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `material_note`;
CREATE TABLE `material_note` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `material_id`      BIGINT   NOT NULL                                COMMENT '被批注的资料 ID（→ library_material.id）',
    `user_id`          BIGINT   NOT NULL                                COMMENT '批注人',
    `note_content`     TEXT                                                 COMMENT '批注内容',
    `highlighted_text` TEXT                                                 COMMENT '高亮的原文片段（用于定位批注位置）',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_material` (`material_id`, `user_id`)                        COMMENT '按资料-用户联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料批注表：立法工作者可在某段资料上留下自己的笔记';

-- =====================================================================
-- 种子数据：流程模板
--   说明：把《规章制定程序条例》《行政法规制定程序条例》做成可配置数据
-- =====================================================================

-- 地方政府规章：依据《规章制定程序条例》
-- 顺序：立项 → 起草 → 征求意见 → 审查 → 决定 → 公布 → 备案
INSERT INTO `legislative_stage_template` (`type`, `stage_code`, `stage_name`, `stage_order`, `default_days`, `is_required`) VALUES
('LOCAL_RULE', 'PROPOSAL',     '立项',         1, 30,  1),
('LOCAL_RULE', 'DRAFTING',     '起草',         2, 90,  1),
('LOCAL_RULE', 'CONSULTATION', '征求意见',     3, 60,  1),
('LOCAL_RULE', 'REVIEW',       '审查',         4, 30,  1),
('LOCAL_RULE', 'DECISION',     '决定',         5, 30,  1),
('LOCAL_RULE', 'PUBLISH',      '公布',         6, 15,  1),
('LOCAL_RULE', 'FILE',         '备案',         7, 30,  1);

-- 部门规章：依据《规章制定程序条例》
-- 顺序：立项 → 起草 → 征求意见 → 审查 → 决定 → 公布 → 备案
INSERT INTO `legislative_stage_template` (`type`, `stage_code`, `stage_name`, `stage_order`, `default_days`, `is_required`) VALUES
('DEPT_RULE', 'PROPOSAL',     '立项',         1, 30,  1),
('DEPT_RULE', 'DRAFTING',     '起草',         2, 120, 1),
('DEPT_RULE', 'CONSULTATION', '征求意见',     3, 60,  1),
('DEPT_RULE', 'REVIEW',       '审查',         4, 30,  1),
('DEPT_RULE', 'DECISION',     '决定',         5, 30,  1),
('DEPT_RULE', 'PUBLISH',      '公布',         6, 15,  1),
('DEPT_RULE', 'FILE',         '备案',         7, 30,  1);

-- 行政法规：依据《行政法规制定程序条例》
-- 顺序：立项 → 起草 → 征求意见 → 审查 → 国务院常务会议审议 → 公布 → 备案
INSERT INTO `legislative_stage_template` (`type`, `stage_code`, `stage_name`, `stage_order`, `default_days`, `is_required`) VALUES
('ADMIN_REGULATION', 'PROPOSAL',      '立项',                   1, 30,  1),
('ADMIN_REGULATION', 'DRAFTING',      '起草',                   2, 180, 1),
('ADMIN_REGULATION', 'CONSULTATION',  '征求意见',               3, 60,  1),
('ADMIN_REGULATION', 'REVIEW',        '审查',                   4, 60,  1),
('ADMIN_REGULATION', 'STATE_COUNCIL', '国务院常务会议审议',     5, 60,  1),
('ADMIN_REGULATION', 'PUBLISH',       '公布',                   6, 15,  1),
('ADMIN_REGULATION', 'FILE',          '备案',                   7, 30,  1);

-- =====================================================================
-- 种子数据：默认评估指标（合法性 / 落实性 / 满意度 三维度，权重合计 1.0）
-- =====================================================================

INSERT INTO `evaluation_indicator` (`indicator_name`, `dimension`, `weight`, `unit`, `formula`, `data_source`) VALUES
('与上位法冲突条款数',    'LEGALITY',     0.20, 'ABSOLUTE', 'count(conflict_articles)',     'review_rule'),
('同位法重复条款数',      'LEGALITY',     0.10, 'ABSOLUTE', 'count(duplicate_articles)',    'review_rule'),
('超出权限范围条款数',    'LEGALITY',     0.10, 'ABSOLUTE', 'count(over_power_articles)',   'review_rule'),
('行政相对人执行率',      'EXECUTION',    0.15, 'RATIO',    'sum(compliant_cases)/sum(total_cases)', 'case_info'),
('平均法规援引率',        'EXECUTION',    0.10, 'RATIO',    'avg(citation_count)',          'case_info'),
('执法案件数（同期）',    'EXECUTION',    0.10, 'ABSOLUTE', 'count(regulation_cases)',      'case_info'),
('行政复议胜诉率',        'EXECUTION',    0.10, 'RATIO',    'sum(uphold)/sum(total)',       'reconsideration'),
('公众满意度',            'SATISFACTION', 0.15, 'SCORE',    'avg(satisfaction_score)',     'survey');

-- =====================================================================
-- 种子数据：审查规则
--   6 类规则，按严重度排序
-- =====================================================================

INSERT INTO `review_rule` (`rule_code`, `rule_name`, `rule_type`, `check_logic`, `severity`) VALUES
('SUPERIOR_CONFLICT',  '与上位法冲突检测',   'LEGAL',  '{"op":"detect_conflict","scope":"superior"}',     'RED'),
('OVER_POWER',         '越权立法检测',       'LEGAL',  '{"op":"detect_overpower","scope":"authority"}',   'RED'),
('OUTDATED_REF',       '失效引用检测',       'LEGAL',  '{"op":"detect_outdated_ref"}',                    'YELLOW'),
('DUPLICATE_ARTICLE',  '同位法重复条款检测', 'LEGAL',  '{"op":"detect_duplicate_article"}',                'YELLOW'),
('FORMAT_FORMAT',      '章节格式规范检测',   'FORMAT', '{"op":"check_chapter_format"}',                   'BLUE'),
('VERBOSE_TEXT',       '条款冗长 AI 建议',   'AI',     '{"op":"ai_verbose_check","model":"draft-llm"}',    'GREY');

-- =====================================================================
-- 种子数据：常用立法资料标签
-- =====================================================================

INSERT INTO `library_tag` (`tag_name`, `tag_type`) VALUES
('行政许可',   'DOMAIN'),
('行政处罚',   'DOMAIN'),
('行政强制',   'DOMAIN'),
('行政确认',   'DOMAIN'),
('行政征收',   'DOMAIN'),
('行政检查',   'DOMAIN'),
('行政复议',   'DOMAIN'),
('政府规章',   'LEVEL'),
('部门规章',   'LEVEL'),
('行政法规',   'LEVEL');

-- =====================================================================
-- 公共：通知消息（DeadlineReminderTask 落地）
--   一条记录 = 一条发往某个用户的通知
--   通道(channel)决定它最终走站内信 / 邮件 / WebSocket
--   接收人由 X-User-Id 头决定（开发期暂时全部写 NULL）
-- =====================================================================

DROP TABLE IF EXISTS `notify_message`;
CREATE TABLE `notify_message` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `recipient_id`  BIGINT                                                COMMENT '接收人用户 ID（NULL = 系统广播）',
    `type`          VARCHAR(32)  NOT NULL                                COMMENT '消息类型：DEADLINE 期限 / CLEANUP 清理 / EVALUATION 评估 / SYSTEM 系统',
    `title`         VARCHAR(255) NOT NULL                                COMMENT '消息标题',
    `content`       TEXT                                                 COMMENT '消息正文',
    `biz_type`      VARCHAR(64)                                          COMMENT '关联业务实体类型：legislative_project / cleanup_task ...',
    `biz_id`        BIGINT                                                COMMENT '关联业务实体 ID',
    `channel`       VARCHAR(32)  NOT NULL DEFAULT 'CONSOLE'              COMMENT '发送通道：CONSOLE / INBOX / EMAIL / WEBSOCKET',
    `is_read`       TINYINT      NOT NULL DEFAULT 0                      COMMENT '是否已读：0 未读 / 1 已读',
    `sent_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipient` (`recipient_id`, `sent_at`)                      COMMENT '接收人+时间联合索引，用于"我的消息"分页',
    KEY `idx_type`      (`type`)                                          COMMENT '按类型过滤',
    KEY `idx_biz`       (`biz_type`, `biz_id`)                            COMMENT '业务实体定位索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息落地表，后续消息中心读取此表';

-- =====================================================================
-- 公共：系统用户（最小可用登录态）
--   立法版先用一张轻量表承载登录身份；后续接 legislation_commons 后可换统一 IdP
-- =====================================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL                                COMMENT '登录用户名',
    `display_name`  VARCHAR(64)                                          COMMENT '显示名',
    `password_hash` VARCHAR(128) NOT NULL                                COMMENT 'BCrypt 密码哈希',
    `role`          VARCHAR(32)  NOT NULL DEFAULT 'ROLE_USER'            COMMENT '角色：ROLE_USER 立法工作者 / ROLE_ADMIN 管理员 / ROLE_LEADER 领导 / ROLE_REVIEWER 审查员 / ROLE_EVALUATOR 评估员',
    `department`    VARCHAR(128)                                          COMMENT '所属部门',
    `is_active`     TINYINT      NOT NULL DEFAULT 1                      COMMENT '是否启用：1 启用 / 0 禁用',
    `last_login_at` DATETIME                                              COMMENT '最后登录时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表：立法版最小登录态';

-- 种子用户:开发期 5 个不同角色的测试账号
-- 密码默认 123456,启动时由 DataInitializer 用 BCrypt 加密后回填 password_hash
INSERT INTO `sys_user` (`username`, `display_name`, `role`, `department`) VALUES
('admin',     '系统管理员', 'ROLE_ADMIN',      '信息中心'),
('leader',    '王主任',     'ROLE_LEADER',     '法规处'),
('drafter',   '李起草员',   'ROLE_USER',       '法规一处'),
('reviewer',  '张审查员',   'ROLE_REVIEWER',   '法制科'),
('evaluator', '赵评估员',   'ROLE_EVALUATOR',  '执法监督局');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 验证：执行完应输出 21/8/6/10 等数字
-- =====================================================================
SELECT 'legislative_stage_template' AS table_name, COUNT(*) AS cnt FROM legislative_stage_template
UNION ALL SELECT 'evaluation_indicator',  COUNT(*) FROM evaluation_indicator
UNION ALL SELECT 'review_rule',           COUNT(*) FROM review_rule
UNION ALL SELECT 'library_tag',           COUNT(*) FROM library_tag
UNION ALL SELECT 'sys_user',              COUNT(*) FROM sys_user;