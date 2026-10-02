-- =====================================================================
-- 智立法 - 集成测试专用 schema (H2 + MySQL 兼容最小子集)
--
-- 只保留 IntegrationTest 用到的表:
--     sys_user
--     legislative_project
--     regulation
--     legislative_stage
--     legislative_draft
--     review_record
--     cleanup_task
--     evaluation_task
--     consultation
--     library_material
--     (其余由后端 mapper 触发的表)
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    display_name    VARCHAR(128),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL DEFAULT 'USER',
    department      VARCHAR(128),
    is_active       TINYINT      NOT NULL DEFAULT 1,
    last_login_at   DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS legislative_project (
    id              BIGINT       NOT NULL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    project_type    VARCHAR(32)  NOT NULL,
    description     TEXT,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    publish_date    DATE,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS legislative_stage (
    id              BIGINT       NOT NULL PRIMARY KEY,
    project_id      BIGINT       NOT NULL,
    stage_name      VARCHAR(64)  NOT NULL,
    stage_order     INT          NOT NULL,
    stage_type      VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    started_at      DATETIME,
    completed_at    DATETIME,
    owner_user_id   BIGINT,
    remark          TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS legislative_draft (
    id              BIGINT       NOT NULL PRIMARY KEY,
    project_id      BIGINT       NOT NULL,
    stage_id        BIGINT,
    title           VARCHAR(255) NOT NULL,
    version         VARCHAR(32),
    content         CLOB,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    ai_generated    TINYINT      NOT NULL DEFAULT 0,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review_record (
    id              BIGINT       NOT NULL PRIMARY KEY,
    draft_id        BIGINT       NOT NULL,
    reviewer_id     BIGINT,
    review_type     VARCHAR(32),
    review_scope    VARCHAR(255),
    overall_score   DECIMAL(5,2),
    summary         CLOB,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review_issue (
    id              BIGINT       NOT NULL PRIMARY KEY,
    review_id       BIGINT       NOT NULL,
    rule_code       VARCHAR(64),
    severity        VARCHAR(16),
    category        VARCHAR(64),
    article_no      VARCHAR(64),
    snippet         CLOB,
    issue_text      CLOB,
    suggestion      CLOB,
    status          VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cleanup_task (
    id              BIGINT       NOT NULL PRIMARY KEY,
    task_name       VARCHAR(255) NOT NULL,
    task_type       VARCHAR(32),
    scope_filter    CLOB,
    total_count     INT          NOT NULL DEFAULT 0,
    processed_count INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    started_at      DATETIME,
    finished_at     DATETIME,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cleanup_suggestion (
    id              BIGINT       NOT NULL PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    target_id       BIGINT,
    target_type     VARCHAR(32),
    regulation_id   BIGINT,
    conflict_type   VARCHAR(64),
    confidence      DECIMAL(5,2),
    evidence        CLOB,
    suggestion      CLOB,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regulation (
    id              BIGINT       NOT NULL PRIMARY KEY,
    regulation_name VARCHAR(255) NOT NULL,
    regulation_type VARCHAR(32)  NOT NULL,
    issuing_authority VARCHAR(128),
    issue_number    VARCHAR(128),
    issue_date      DATE,
    effective_date  DATE,
    expire_date     DATE,
    status          VARCHAR(32)  NOT NULL DEFAULT 'EFFECTIVE',
    full_text       CLOB,
    digest          CLOB,
    source_url      VARCHAR(512),
    region_code     VARCHAR(16),
    is_from_crawler TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_task (
    id              BIGINT       NOT NULL PRIMARY KEY,
    task_name       VARCHAR(255) NOT NULL,
    regulation_id   BIGINT,
    eval_year       INT,
    scope_desc      CLOB,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    ai_score        DECIMAL(5,2),
    ai_summary      CLOB,
    final_score     DECIMAL(5,2),
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consultation (
    id              BIGINT       NOT NULL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    topic           VARCHAR(255),
    status          VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    start_date      DATE,
    end_date        DATE,
    description     CLOB,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opinion (
    id              BIGINT       NOT NULL PRIMARY KEY,
    consultation_id BIGINT       NOT NULL,
    submitter       VARCHAR(128),
    submitter_role  VARCHAR(64),
    content         CLOB,
    sentiment       VARCHAR(32),
    is_anonymous    TINYINT      NOT NULL DEFAULT 0,
    reply_count     INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS library_material (
    id              BIGINT       NOT NULL PRIMARY KEY,
    material_title  VARCHAR(255) NOT NULL,
    material_type   VARCHAR(32),
    region_code     VARCHAR(16),
    author          VARCHAR(128),
    source          VARCHAR(255),
    publish_date    DATE,
    content         CLOB,
    digest          CLOB,
    attachment_url  VARCHAR(512),
    view_count      INT          NOT NULL DEFAULT 0,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS legislative_deadline (
    id              BIGINT       NOT NULL PRIMARY KEY,
    project_id      BIGINT       NOT NULL,
    stage_id        BIGINT,
    deadline_type   VARCHAR(32),
    deadline_at     DATETIME     NOT NULL,
    remind_days     INT,
    remind_sent     TINYINT      NOT NULL DEFAULT 0,
    owner_user_id   BIGINT,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS library_tag (
    id              BIGINT       NOT NULL PRIMARY KEY,
    tag_name        VARCHAR(64)  NOT NULL,
    tag_category    VARCHAR(64),
    usage_count     INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_tag (
    id              BIGINT       NOT NULL PRIMARY KEY,
    material_id     BIGINT       NOT NULL,
    tag_id          BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_note (
    id              BIGINT       NOT NULL PRIMARY KEY,
    material_id     BIGINT       NOT NULL,
    user_id         BIGINT,
    note_text       CLOB,
    note_type       VARCHAR(32),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regulation_relation (
    id              BIGINT       NOT NULL PRIMARY KEY,
    source_regulation_id BIGINT  NOT NULL,
    target_regulation_id BIGINT  NOT NULL,
    relation_type   VARCHAR(32)  NOT NULL,
    description     CLOB,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS legislative_stage_template (
    id              BIGINT       NOT NULL PRIMARY KEY,
    template_name   VARCHAR(64)  NOT NULL,
    project_type    VARCHAR(32)  NOT NULL,
    stage_count     INT          NOT NULL DEFAULT 0,
    description     CLOB,
    is_active        TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS draft_version_history (
    id              BIGINT       NOT NULL PRIMARY KEY,
    draft_id        BIGINT       NOT NULL,
    version_no      INT          NOT NULL,
    content_snapshot CLOB,
    change_summary  CLOB,
    editor_id       BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opinion_category (
    id              BIGINT       NOT NULL PRIMARY KEY,
    consultation_id BIGINT       NOT NULL,
    category_name   VARCHAR(64)  NOT NULL,
    category_color  VARCHAR(16),
    description     CLOB,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opinion_reply (
    id              BIGINT       NOT NULL PRIMARY KEY,
    opinion_id      BIGINT       NOT NULL,
    replier_id      BIGINT,
    replier_name    VARCHAR(128),
    content         CLOB,
    is_official     TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review_rule (
    id              BIGINT       NOT NULL PRIMARY KEY,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(255) NOT NULL,
    rule_type       VARCHAR(32),
    severity        VARCHAR(16),
    description     CLOB,
    pattern         CLOB,
    enabled         TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_indicator (
    id              BIGINT       NOT NULL PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    indicator_name  VARCHAR(128) NOT NULL,
    dimension       VARCHAR(32),
    weight          DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    score           DECIMAL(5,2),
    description     CLOB,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_result (
    id              BIGINT       NOT NULL PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    indicator_id    BIGINT,
    score           DECIMAL(5,2),
    ai_comment      CLOB,
    evidence        CLOB,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_project_status    ON legislative_project(status);
CREATE INDEX idx_project_type      ON legislative_project(project_type);
CREATE INDEX idx_stage_project      ON legislative_stage(project_id);
CREATE INDEX idx_draft_project      ON legislative_draft(project_id);
CREATE INDEX idx_review_draft       ON review_record(draft_id);
CREATE INDEX idx_issue_review       ON review_issue(review_id);
CREATE INDEX idx_cleanup_status     ON cleanup_task(status);
CREATE INDEX idx_suggestion_task    ON cleanup_suggestion(task_id);
CREATE INDEX idx_eval_task          ON evaluation_task(status);
CREATE INDEX idx_consultation_status ON consultation(status);
CREATE INDEX idx_opinion_consult     ON opinion(consultation_id);
CREATE INDEX idx_library_type       ON library_material(material_type);
CREATE INDEX idx_deadline_at        ON legislative_deadline(deadline_at);
CREATE INDEX idx_tag_material       ON material_tag(material_id);

-- 种子数据(集成测试用)
INSERT INTO sys_user (id, username, display_name, password_hash, role, is_active)
VALUES
    (1, 'admin', '系统管理员', '$2a$10$NV0H8qg1kTFEEMxRGMrSkO3KBkLxxmIK7C9.NlpCjjGVRnpbqONHK', 'ADMIN', 1),
    (2, 'user1', '立法专员',   '$2a$10$NvH8qFK5J8LqKpR8D5YYj.kVwqAkT2PJ9SjN3KhL2vN3kFvWpYfPm', 'USER',  1);

INSERT INTO legislative_project (id, project_name, project_type, description, status, created_by)
VALUES
    (1001, '网络数据安全管理条例', 'ADMIN_REGULATION', '规范网络数据处理活动', 'ACTIVE', 1),
    (1002, '某省数据交易管理办法', 'LOCAL_RULE',       '规范本省数据交易行为', 'ACTIVE', 1);

INSERT INTO regulation (id, regulation_name, regulation_type, issuing_authority, status, region_code)
VALUES
    (1, '中华人民共和国数据安全法', 'ADMIN_REGULATION', '全国人大常委会', 'EFFECTIVE', '000000'),
    (2, '中华人民共和国个人信息保护法', 'ADMIN_REGULATION', '全国人大常委会', 'EFFECTIVE', '000000');