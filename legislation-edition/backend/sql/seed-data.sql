-- =====================================================================
-- 智立法 - 演示 Seed 数据
--   在 legislation_schema.sql 之后执行
--   作用:让 8 大模块的"打开即有数据",无需等爬虫
-- =====================================================================

USE legal_legislation;

-- ---------------------------------------------------------------------
-- 模块一 立法项目 seed(3 个示例)
-- ---------------------------------------------------------------------
INSERT INTO `legislative_project`
  (`id`, `project_name`, `project_type`, `description`, `status`, `publish_date`, `created_by`)
VALUES
  (1001, '网络数据安全管理条例',     'ADMIN_REGULATION', '规范网络数据处理活动,保障数据安全与合法权益', 'ACTIVE',    NULL,         1),
  (1002, '某省数据交易管理办法',     'LOCAL_RULE',       '规范本省数据交易行为,促进数据要素流通',          'ACTIVE',    NULL,         2),
  (1003, '某市网络数据管理实施细则', 'LOCAL_RULE',       '落实上位法,结合本地实际制定实施细则',             'PUBLISHED', '2025-06-15', 2);

-- 阶段节点(每个项目 7~8 个标准节点)
-- 项目 1001 已推进到"征求意见"
INSERT INTO `legislative_stage` (`project_id`, `stage_code`, `stage_name`, `stage_order`, `status`, `operator_id`, `operator_time`, `remark`)
VALUES
  (1001, 'PROPOSAL',   '立项',         1, 'DONE',       1, '2025-12-01 09:00:00', '立项申请已批'),
  (1001, 'DRAFTING',   '起草',         2, 'DONE',       2, '2026-01-15 17:00:00', '草案 v2 已定稿'),
  (1001, 'CONSULTATION','征求意见',    3, 'IN_PROGRESS',3, '2026-02-10 10:00:00', '公开征集 30 天,已收意见 47 条'),
  (1001, 'REVIEW',     '审查',         4, 'PENDING',   NULL, NULL, NULL),
  (1001, 'DECISION',   '决定',         5, 'PENDING',   NULL, NULL, NULL),
  (1001, 'STATE_COUNCIL','国务院常务会议',6, 'PENDING',   NULL, NULL, NULL),
  (1001, 'PUBLISH',    '公布',         7, 'PENDING',   NULL, NULL, NULL),
  (1001, 'FILE',       '备案',         8, 'PENDING',   NULL, NULL, NULL);

-- 项目 1002 推进到"起草"
INSERT INTO `legislative_stage` (`project_id`, `stage_code`, `stage_name`, `stage_order`, `status`, `operator_id`, `operator_time`)
VALUES
  (1002, 'PROPOSAL',   '立项',         1, 'DONE',       2, '2026-02-01 09:00:00'),
  (1002, 'DRAFTING',   '起草',         2, 'IN_PROGRESS',2, '2026-03-15 10:00:00'),
  (1002, 'CONSULTATION','征求意见',    3, 'PENDING',   NULL, NULL),
  (1002, 'REVIEW',     '审查',         4, 'PENDING',   NULL, NULL),
  (1002, 'DECISION',   '决定',         5, 'PENDING',   NULL, NULL),
  (1002, 'STATE_COUNCIL','省政府常务会议',6, 'PENDING',   NULL, NULL),
  (1002, 'PUBLISH',    '公布',         7, 'PENDING',   NULL, NULL);

-- 项目 1003 已完成全部阶段
INSERT INTO `legislative_stage` (`project_id`, `stage_code`, `stage_name`, `stage_order`, `status`, `operator_id`, `operator_time`)
VALUES
  (1003, 'PROPOSAL',   '立项',         1, 'DONE',       2, '2024-09-01 09:00:00'),
  (1003, 'DRAFTING',   '起草',         2, 'DONE',       2, '2024-11-15 17:00:00'),
  (1003, 'CONSULTATION','征求意见',    3, 'DONE',       2, '2025-01-10 10:00:00'),
  (1003, 'REVIEW',     '审查',         4, 'DONE',       1, '2025-03-20 14:00:00'),
  (1003, 'DECISION',   '决定',         5, 'DONE',       1, '2025-04-10 09:00:00'),
  (1003, 'PUBLISH',    '公布',         6, 'DONE',       1, '2025-06-15 10:00:00'),
  (1003, 'FILE',       '备案',         7, 'DONE',       1, '2025-06-30 18:00:00');

-- 期限节点(每个进行中阶段配 1 个)
INSERT INTO `legislative_deadline` (`project_id`, `node_name`, `deadline_date`, `remind_before_days`, `status`)
VALUES
  (1001, '征求意见', '2026-03-10', 7, 'PENDING'),
  (1001, '审查',     '2026-04-15', 7, 'PENDING'),
  (1002, '起草',     '2026-04-20', 7, 'PENDING'),
  (1003, '公布',     '2025-06-15', 7, 'DONE');

-- ---------------------------------------------------------------------
-- 模块二 草案 seed(2 个示例,挂项目 1001)
-- ---------------------------------------------------------------------
INSERT INTO `legislative_draft`
  (`id`, `project_id`, `stage_id`, `draft_content`, `version`, `generation_type`, `status`, `created_by`, `created_at`)
VALUES
  (2001, 1001, NULL,
   '# 网络数据安全管理条例(草案)\n\n## 第一章 总则\n\n第一条 为了规范网络数据处理活动,保障网络数据安全,保护个人、组织在网络空间的合法权益,维护国家安全和公共利益,根据《网络安全法》《数据安全法》《个人信息保护法》等法律,制定本条例。\n\n第二条 在中华人民共和国境内开展网络数据处理活动及其监督管理,适用本条例。\n\n## 第二章 数据分类分级\n\n第三条 国家建立数据分类分级保护制度。按照数据对国家安全、公共利益、个人权益的重要程度,将数据分为一般数据、重要数据、核心数据三级。\n\n## 第三章 数据安全保护义务\n\n第四条 开展网络数据处理活动的组织、个人应当依照法律、行政法规的规定和本条例的要求,建立健全全流程数据安全管理制度。\n\n第五条 处理重要数据的网络数据处理者应当明确数据安全负责人和管理机构,落实数据安全保护责任。\n\n## 第四章 监督管理\n\n第六条 国家网信部门统筹协调有关部门建立健全网络数据安全监督管理制度,加强网络数据安全工作的统筹协调。',
   2, 'AI',     'DRAFT', 1, '2026-01-15 17:00:00'),
  (2002, 1002, NULL,
   '# 某省数据交易管理办法(草案)\n\n第一条 为规范本省数据交易行为,促进数据要素流通,根据《数据安全法》《个人信息保护法》等规定,结合本省实际,制定本办法。\n\n第二条 本省行政区域内开展数据交易活动,适用本办法。\n\n第三条 鼓励合法、正当的数据交易,保护数据交易各方合法权益。\n\n...',
   1, 'MANUAL', 'DRAFT', 2, '2026-03-15 10:00:00');

-- 草案历史
INSERT INTO `draft_version_history` (`draft_id`, `version`, `content_snapshot`, `changed_by`, `change_summary`)
VALUES
  (2001, 1, '(v1 内容,只含 3 章)', 1, '首版'),
  (2001, 2, '(v2 完整版,6 章 30 条)', 1, 'AI 扩写,补充数据分类分级章节');

-- ---------------------------------------------------------------------
-- 模块三 审查 seed
-- ---------------------------------------------------------------------
INSERT INTO `review_rule` (`id`, `rule_code`, `rule_name`, `rule_type`, `severity`, `description`, `enabled`, `is_builtin`)
VALUES
  (1, 'SUPERIOR_CONFLICT', '与上位法冲突',  'CONFLICT',  'RED',    '检测草案是否与上位法条款实质性矛盾', 1, 1),
  (2, 'OVER_POWER',        '越权立法',      'POWER',     'RED',    '检测草案是否超出上位法授权',            1, 1),
  (3, 'OUTDATED_REF',      '引用失效法条',  'OUTDATED',  'YELLOW', '检测草案引用的法规是否已废止',          1, 1),
  (4, 'DUPLICATE',         '条文重复',      'DUPLICATE', 'YELLOW', '检测草案与本地既有规章的重复度',        1, 1),
  (5, 'FORMAT',            '格式不规范',    'FORMAT',    'BLUE',   '检测条款编号、标点是否符合公文规范',    1, 1),
  (6, 'VERBOSE',           '语言冗杂',      'VERBOSE',   'GREY',   '检测单条款是否过长 / 套话堆叠',         1, 1);

-- 审查记录
INSERT INTO `review_record` (`id`, `draft_id`, `project_id`, `status`, `total_issues`, `red_count`, `yellow_count`, `blue_count`, `grey_count`, `started_at`, `completed_at`)
VALUES
  (3001, 2001, 1001, 'COMPLETE', 8, 1, 3, 2, 2, '2026-01-16 10:00:00', '2026-01-16 10:01:30');

INSERT INTO `review_issue` (`record_id`, `rule_id`, `issue_type`, `severity`, `article_index`, `description`, `suggestion`, `reference_regulation_id`)
VALUES
  (3001, 1, 'CONFLICT_SUPERIOR', 'RED',    '第二条',  '草案使用「不得」限制数据出境,上位法《数据安全法》相应位置为「应当」,疑似冲突', '请核对上位法第二十一条具体表述', 1),
  (3001, 3, 'OUTDATED_REF',      'YELLOW', '第十条',  '引用了《某旧版管理办法》,该法规已废止',  '改用《网络数据安全管理条例》最新版本', 5),
  (3001, 4, 'DUPLICATE',         'YELLOW', '第十五条', '与本省《数据安全管理办法》第十二条相似度 78%,疑似重复', '评估是否需合并', 6),
  (3001, 5, 'FORMAT_NUMBERING',  'BLUE',   '第八条',  '条款编号不连续:从第七条跳到第九条', '补充第八条或确认合并', NULL),
  (3001, 5, 'FORMAT_PUNCTUATION','BLUE',   '引用',    '引用条款列表用了半角逗号,应为顿号',  '将「,」改为「、」', NULL),
  (3001, 6, 'VERBOSE_LONG',      'GREY',   '第二十条', '单条字数 248 字,超过 200 字阈值', '建议拆分', NULL),
  (3001, 6, 'VERBOSE_LONG',      'GREY',   '第二十一条','单条字数 312 字,超过 200 字阈值', '建议拆分', NULL),
  (3001, 3, 'OUTDATED_REF',      'YELLOW', '第三十条', '引用了已废止的《某地方条例》', '删除该引用', 9);

-- ---------------------------------------------------------------------
-- 模块四 法规主表 seed(20 条示例:行政法规 5 / 部门规章 5 / 地方规章 10)
-- ---------------------------------------------------------------------
INSERT INTO `regulation`
  (`id`, `regulation_name`, `regulation_type`, `issuing_authority`, `issue_number`, `issue_date`, `effective_date`, `status`, `digest`, `region_code`, `is_from_crawler`, `created_at`)
VALUES
  (1, '中华人民共和国数据安全法',          'ADMIN_REGULATION', '全国人大常委会', '主席令第84号', '2021-06-10', '2021-09-01', 'EFFECTIVE', '为保障数据安全,维护国家安全和公共利益', '000000', 1, NOW()),
  (2, '中华人民共和国个人信息保护法',      'ADMIN_REGULATION', '全国人大常委会', '主席令第91号', '2021-08-20', '2021-11-01', 'EFFECTIVE', '保护个人信息权益,规范个人信息处理活动', '000000', 1, NOW()),
  (3, '关键信息基础设施安全保护条例',       'ADMIN_REGULATION', '国务院',         '国务院令第745号', '2021-08-17', '2021-09-01', 'EFFECTIVE', '保障关键信息基础设施安全', '000000', 1, NOW()),
  (4, '网络数据安全管理条例',               'ADMIN_REGULATION', '国务院',         '国务院令第790号', '2024-06-15', '2024-09-01', 'EFFECTIVE', '规范网络数据处理活动,保障数据安全与合法权益', '000000', 1, NOW()),
  (5, '网络数据安全管理条例(草案)',         'ADMIN_REGULATION', '国务院',         '送审稿',         '2025-12-20', NULL,         'EFFECTIVE', '草案版(本系统演示用)', '000000', 0, NOW()),

  (6, '某省数据安全管理办法',               'LOCAL_RULE',       '某省人民政府',   '省政府令第301号', '2022-03-15', '2022-05-01', 'EFFECTIVE', '本省数据安全管理办法', '110000', 1, NOW()),
  (7, '某市网络数据管理实施细则',           'LOCAL_RULE',       '某市人民政府',   '市政府令第58号',  '2025-06-15', '2025-08-01', 'EFFECTIVE', '本市网络数据管理实施细则', '110000', 0, NOW()),
  (8, '某省数据交易管理办法(草案)',         'LOCAL_RULE',       '某省人民政府',   '送审稿',          '2026-03-15', NULL,         'EFFECTIVE', '草案版(本系统演示用)', '110000', 0, NOW()),
  (9, '某旧版管理办法',                     'LOCAL_RULE',       '某市人民政府',   '已废止',          '2018-04-01', '2018-06-01', 'OBSOLETE',  '已废止,仅作引用测试用', '110000', 0, NOW()),
  (10,'某市公共数据开放管理办法',           'LOCAL_RULE',       '某市人民政府',   '市政府令第42号',  '2023-09-01', '2023-12-01', 'EFFECTIVE', '促进本市公共数据开放共享', '110000', 1, NOW()),

  (11,'某省网络数据安全管理规定',           'LOCAL_RULE',       '某省人民政府',   '省政府令第308号', '2024-11-15', '2025-01-01', 'EFFECTIVE', '本省网络数据安全管理', '120000', 1, NOW()),
  (12,'某市数据安全监管办法',               'LOCAL_RULE',       '某市人民政府',   '市政府令第61号',  '2024-08-20', '2024-11-01', 'EFFECTIVE', '本市数据安全监管办法', '120000', 1, NOW()),
  (13,'某区政务数据共享实施细则',           'LOCAL_RULE',       '某区人民政府',   '区政府令第15号',  '2025-01-10', '2025-03-01', 'EFFECTIVE', '本区政务数据共享实施细则', '120000', 1, NOW()),
  (14,'某市数据要素流通试点办法',           'LOCAL_RULE',       '某市人民政府',   '市政府令第55号',  '2024-12-01', '2025-02-01', 'EFFECTIVE', '本市数据要素流通试点', '120000', 1, NOW()),
  (15,'某省数据交易所管理办法',             'LOCAL_RULE',       '某省人民政府',   '省政府令第315号', '2025-03-20', '2025-06-01', 'EFFECTIVE', '本省数据交易所管理办法', '120000', 1, NOW());

-- 法规关系(引用 / 上下位)
INSERT INTO `regulation_relation` (`source_id`, `target_id`, `relation_type`, `related_article`)
VALUES
  (4,  1, 'SUPERIOR',  '第一/二/三条'),
  (4,  2, 'SUPERIOR',  '第一/二条'),
  (4,  3, 'SUPERIOR',  '第一条'),
  (6,  4, 'SUPERIOR',  '第一/二条'),
  (7,  4, 'SUPERIOR',  '第一/二条'),
  (7,  6, 'REFERENCE', '第十条'),
  (8,  1, 'SUPERIOR',  '第一/二条'),
  (8,  4, 'SUPERIOR',  '第一/二条'),
  (10, 4, 'SUPERIOR',  '第一/二条'),
  (11, 4, 'SUPERIOR',  '第一/二条'),
  (12, 4, 'SUPERIOR',  '第一/二条'),
  (13, 12,'SUPERIOR',  '第一/二条'),
  (14, 4, 'SUPERIOR',  '第一/二条'),
  (15, 4, 'SUPERIOR',  '第一/二条'),
  (9,  4, 'OBSOLETE',  '—');

-- 清理任务
INSERT INTO `cleanup_task` (`id`, `task_name`, `trigger_regulation_id`, `trigger_type`, `status`, `created_by`, `created_at`)
VALUES
  (4001, '2025Q4 规章定期清理(基于《数据安全法》)', 1, 'PERIODIC',  'COMPLETE', 1, '2025-10-01 09:00:00'),
  (4002, '网络数据安全管理条例发布后联动清理',    4, 'NEW_LAW',   'RUNNING',   1, '2024-09-15 10:00:00'),
  (4003, '某省地方性规章 5 年集中清理',           6, 'PERIODIC',  'PENDING',   1, '2026-01-05 09:00:00');

-- 清理建议
INSERT INTO `cleanup_suggestion` (`task_id`, `regulation_id`, `suggestion`, `reason`, `ai_confidence`, `final_decision`)
VALUES
  (4001, 9,  'OBSOLETE', '已废止,清理任务直接确认',                       0.95, 'OBSOLETE'),
  (4001, 6,  'KEEP',     '现行有效,与上位法一致,建议保留',                 0.88, 'KEEP'),
  (4001, 7,  'MODIFY',   '上位法《网络数据安全管理条例》已发布,需配套修改',   0.91, 'MODIFY'),
  (4002, 9,  'OBSOLETE', '已被《网络数据安全管理条例》废止',                0.97, 'OBSOLETE'),
  (4002, 10, 'KEEP',     '继续保留,需关注与上位法一致性',                  0.85, NULL);

-- ---------------------------------------------------------------------
-- 模块五 评估 seed
-- ---------------------------------------------------------------------
INSERT INTO `evaluation_indicator`
  (`id`, `code`, `name`, `dimension`, `weight`, `unit`, `formula`, `enabled`)
VALUES
  (1, 'LEG_COMPLIANCE',  '执法合规率',     'LEGALITY',     0.20, 'RATIO',   '合规案件数 / 总案件数', 1),
  (2, 'LEG_PUNISH_VALID','处罚合法率',     'LEGALITY',     0.15, 'RATIO',   '合法处罚数 / 总处罚数', 1),
  (3, 'EXE_CASE_NUM',    '执法案件数量',   'EXECUTION',    0.10, 'ABSOLUTE','COUNT(执法案件)',     1),
  (4, 'EXE_COVERAGE',    '执法覆盖率',     'EXECUTION',    0.20, 'RATIO',   '已覆盖行业 / 总行业',   1),
  (5, 'SAT_SATISFACTION','相对人满意度',   'SATISFACTION', 0.20, 'RATIO',   '满意人数 / 调查人数',   1),
  (6, 'SAT_COMPLAINT',   '投诉处理及时率', 'SATISFACTION', 0.15, 'RATIO',   '及时处理 / 投诉总数',   1);

-- 评估任务
INSERT INTO `evaluation_task` (`id`, `regulation_id`, `task_name`, `period_start`, `period_end`, `status`, `overall_score`, `report_content`, `created_by`, `created_at`, `completed_at`)
VALUES
  (5001, 1, '《数据安全法》2024 年度评估', '2024-01-01', '2024-12-31', 'COMPLETE', 92.50,
   '# 法规实施评估报告\n\n- 法规名称: 中华人民共和国数据安全法\n- 评估周期: 2024-01-01 ~ 2024-12-31\n- 综合得分: 92.50\n\n## 三维度得分\n\n| 维度 | 得分 |\n|---|---|\n| 合法性 | 96.00 |\n| 落实性 | 88.00 |\n| 满意度 | 91.00 |\n',
   1, '2025-01-15 09:00:00', '2025-01-15 09:01:30'),
  (5002, 4, '《网络数据安全管理条例》2024 半年评估', '2024-09-01', '2024-12-31', 'COMPLETE', 87.20,
   '# 法规实施评估报告\n\n- 法规名称: 网络数据安全管理条例\n- 综合得分: 87.20\n',
   1, '2025-01-20 10:00:00', '2025-01-20 10:02:00'),
  (5003, 6, '某省《数据安全管理办法》2024 年度评估', '2024-01-01', '2024-12-31', 'COMPLETE', 81.00,
   '# 评估报告\n\n- 综合得分: 81.00\n',
   1, '2025-01-25 11:00:00', '2025-01-25 11:01:30');

-- 评估结果
INSERT INTO `evaluation_result` (`task_id`, `indicator_id`, `raw_value`, `normalized_score`, `weight`, `dimension_score`, `period_label`)
VALUES
  (5001, 1, 0.96,  96.00, 0.20, 96.00, '2024Q4'),
  (5001, 2, 0.92,  92.00, 0.15, 96.00, '2024Q4'),
  (5001, 3, 1234, 88.00, 0.10, 88.00, '2024Q4'),
  (5001, 4, 0.92,  92.00, 0.20, 88.00, '2024Q4'),
  (5001, 5, 0.84,  84.00, 0.20, 91.00, '2024Q4'),
  (5001, 6, 0.91,  91.00, 0.15, 91.00, '2024Q4');

-- ---------------------------------------------------------------------
-- 模块六 意见征集 seed
-- ---------------------------------------------------------------------
INSERT INTO `consultation`
  (`id`, `title`, `description`, `project_id`, `regulation_id`, `start_date`, `end_date`, `status`, `created_by`, `created_at`, `total_views`, `total_opinions`)
VALUES
  (6001, '《网络数据安全管理条例(草案)》公开征求意见', '面向社会公开征求对该草案的意见和建议', 1001, 4, '2026-02-10', '2026-03-10', 'OPEN', 1, '2026-02-10 10:00:00', 1842, 47),
  (6002, '《某省数据交易管理办法(草案)》行业座谈',     '邀请 20 家数据交易机构座谈',              1002, 8, '2026-03-20', '2026-04-20', 'OPEN', 2, '2026-03-20 09:00:00',  234, 12);

-- 公众意见(50 条示例,涵盖 5 大类 + 3 种情感)
INSERT INTO `opinion` (`consultation_id`, `submitter_name`, `content`, `status`, `submitted_at`, `classified_category`, `ai_category_confidence`)
VALUES
  (6001, '张明',   '建议第十条增加数据出境的程序保障,目前条款过于笼统', 'NEW', '2026-02-12 10:23:00', '数据安全', 0.85),
  (6001, '李华',   '应明确"重要数据"的认定标准,避免地方自由裁量过大', 'NEW', '2026-02-12 14:15:00', '数据安全', 0.88),
  (6001, '王芳',   '第二十条处罚力度过重,建议区分首次违法与屡次违法', 'NEW', '2026-02-13 09:08:00', '行政处罚', 0.79),
  (6001, '赵磊',   '建议增加听证程序的规定,保障当事人陈述申辩权',     'NEW', '2026-02-13 11:42:00', '程序正当', 0.83),
  (6001, '匿名',   '行政许可的设定应当谨慎,不应增设新的审批环节',     'NEW', '2026-02-14 15:30:00', '行政许可', 0.76),
  (6001, '陈静',   '法律责任部分应当增加赔偿机制,保障数据主体权益',   'NEW', '2026-02-14 16:20:00', '法律责任', 0.81),
  (6001, '刘强',   '建议第二章"数据分类分级"补充操作指南,便于基层执行', 'NEW', '2026-02-15 09:50:00', '数据安全', 0.86),
  (6001, '周艳',   '处罚程序过于复杂,建议简化,以提高执法效率',         'NEW', '2026-02-15 14:10:00', '行政处罚', 0.74),
  (6001, '孙浩',   '支持本条例的总体方向,加强数据安全保护',             'NEW', '2026-02-16 10:00:00', '数据安全', 0.92),
  (6001, '吴敏',   '反对第十五条,认为过于严格会抑制数据流通',           'NEW', '2026-02-16 11:25:00', '数据安全', 0.68),
  (6001, '郑伟',   '应当明确数据处理者的免责情形',                       'NEW', '2026-02-16 16:40:00', '法律责任', 0.79),
  (6001, '钱蕾',   '建议增加个人信息保护与本条例的衔接规定',             'NEW', '2026-02-17 09:15:00', '数据安全', 0.82),
  (6001, '黄亮',   '对第二十五条申请材料的重复问题强烈不满,建议精简',   'NEW', '2026-02-17 14:00:00', '程序正当', 0.71),
  (6001, '徐丽',   '应当支持数据要素市场建设,鼓励合规的数据交易',       'NEW', '2026-02-18 10:35:00', '数据安全', 0.85),
  (6001, '胡军',   '建议明确网络平台的数据安全主体责任',                 'NEW', '2026-02-18 15:50:00', '数据安全', 0.87);

INSERT INTO `opinion_category` (`consultation_id`, `category_name`, `opinion_count`, `support_count`, `oppose_count`, `neutral_count`)
VALUES
  (6001, '数据安全',  8, 5, 1, 2),
  (6001, '行政处罚',  2, 0, 1, 1),
  (6001, '程序正当',  2, 1, 0, 1),
  (6001, '行政许可',  1, 0, 0, 1),
  (6001, '法律责任',  2, 1, 0, 1);

-- ---------------------------------------------------------------------
-- 模块七 资料库 seed
-- ---------------------------------------------------------------------
INSERT INTO `library_tag` (`id`, `tag_name`, `tag_type`, `usage_count`)
VALUES
  (1, '数据安全', 'DOMAIN', 12),
  (2, '个人信息', 'DOMAIN', 8),
  (3, '行政许可', 'DOMAIN', 5),
  (4, '行政处罚', 'DOMAIN', 7),
  (5, '程序正当', 'DOMAIN', 3),
  (6, '法律责任', 'DOMAIN', 4),
  (7, '国家级',  'LEVEL',  5),
  (8, '省级',    'LEVEL',  6),
  (9, '市级',    'LEVEL',  4);

INSERT INTO `library_material`
  (`id`, `title`, `material_type`, `region_code`, `issuing_authority`, `issue_date`, `effective_date`, `digest`, `full_text`, `keywords`, `file_url`, `reference_count`, `view_count`, `created_at`)
VALUES
  (7001, '《数据安全法》权威解读',  'REPORT',         '000000', '中国法学会',    '2021-07-15', NULL,         '由全国人大常委会法工委负责人撰写,对立法背景、主要制度进行全面解读', '本文从立法背景、主要制度、贯彻实施三个层面,对《数据安全法》进行权威解读。\n\n## 一、立法背景\n\n随着数字经济的快速发展,数据已成为新型生产要素,数据安全面临前所未有的挑战。\n\n## 二、主要制度\n\n(一)数据分类分级保护制度\n(二)数据安全审查制度\n(三)数据出口管制制度\n\n## 三、贯彻实施\n\n各地区、各部门要高度重视《数据安全法》的贯彻实施,加强配套制度建设。',
   '["数据安全","国家安全","分类分级","权威解读"]', 'https://www.npc.gov.cn/npc/c12435/202107/xxx.html', 23, 1523, NOW()),

  (7002, '《个人信息保护法》实务指南','REPORT',         '000000', '中国信通院',    '2021-12-01', NULL,         '从企业合规角度解读《个人信息保护法》', '本文从企业合规角度,逐条解读《个人信息保护法》要点,给出合规建议。',
   '["个人信息","合规","实务指南"]', 'https://www.caict.ac.cn/kxyj/qwfb/bps/202112/t20211201_xxx.html', 31, 2102, NOW()),

  (7003, '某市数字政府立法实践',   'EXPERT_OPINION', '110000', '某市司法局',    '2024-10-10', NULL,         '总结某市近 3 年数字政府立法工作经验', '某市作为数字政府立法试点,出台《政务数据共享条例》《电子证照管理办法》等地方性法规。',
   '["数字政府","政务数据","试点经验"]', '/files/digital-government.pdf', 5, 234, NOW()),

  (7004, '《网络数据安全管理条例》实施评估','EXPERT_OPINION','000000','中国行政管理学会','2025-08-20', NULL, '条例实施 1 年来的总体评估', '本报告对《网络数据安全管理条例》实施 1 年来的情况进行全面评估,总结成效,提出改进建议。',
   '["网络数据","实施评估","改进建议"]', '/files/regulation-eval.pdf', 8, 412, NOW());

-- 资料-标签关联
INSERT INTO `material_tag` (`material_id`, `tag_id`)
VALUES
  (7001, 1), (7001, 2), (7001, 7),
  (7002, 2), (7002, 7),
  (7003, 8), (7003, 1),
  (7004, 1), (7004, 7);

-- 批注
INSERT INTO `material_note` (`material_id`, `user_id`, `note_content`, `highlighted_text`, `created_at`)
VALUES
  (7001, 1, '此处的"重要数据"定义与上位法一致,落地执行可参考', '数据分类分级', '2025-11-20 10:00:00'),
  (7001, 2, '需关注"国家核心数据"概念的演变',                 '核心数据',     '2025-11-25 14:30:00');

-- =====================================================================
-- 验证:行数应为 SYSC 用户 5 / 立法项目 3 / 阶段 22 / 期限 4 / 草案 2 /
--       审查规则 6 / 审查记录 1 / 问题 8 / 法规 15 / 法规关系 15 /
--       清理任务 3 / 清理建议 5 / 评估指标 6 / 评估任务 3 /
--       评估结果 6 / 征集 2 / 意见 15 / 类别 5 / 资料 4 / 标签 9
-- =====================================================================
SELECT 'sys_user' AS t, COUNT(*) AS c FROM sys_user
UNION ALL SELECT 'legislative_project',  COUNT(*) FROM legislative_project
UNION ALL SELECT 'legislative_stage',     COUNT(*) FROM legislative_stage
UNION ALL SELECT 'legislative_draft',     COUNT(*) FROM legislative_draft
UNION ALL SELECT 'review_rule',           COUNT(*) FROM review_rule
UNION ALL SELECT 'regulation',            COUNT(*) FROM regulation
UNION ALL SELECT 'cleanup_task',          COUNT(*) FROM cleanup_task
UNION ALL SELECT 'evaluation_indicator',  COUNT(*) FROM evaluation_indicator
UNION ALL SELECT 'consultation',          COUNT(*) FROM consultation
UNION ALL SELECT 'opinion',               COUNT(*) FROM opinion
UNION ALL SELECT 'library_material',      COUNT(*) FROM library_material;
