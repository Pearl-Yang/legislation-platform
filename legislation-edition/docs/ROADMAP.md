# 立法版阶段开发指南（ROADMAP）

> 本文件是 `legislation_edition/README.md` 第 5 节"阶段开发指南"的展开说明。
> 每次开启新阶段前先看本节，确认本阶段交付清单 & 与前后阶段的依赖。

---

## Phase 0 - 目录骨架 ✅ 当前已完成

**目标**：打通"立项 → 表结构 → 流程引擎 → 抓取"的最小可跑通链路。

**关键交付**：
- 24 张业务表的 ER 设计 + `legislation_schema.sql`
- 24 个 MyBatis-Plus 实体类
- 24 个 Mapper（基础 BaseMapper 扩展）
- `LegislativeFlowService` 派生自 `CaseFlowService`：模板驱动 + 节点推进 + 进度计算
- `RegulationGraphService` 派生自 `KnowledgeGraphService`：法规关系子图接口（当前内存实现）
- `crawler/legislation_spider.py`：行政法规 / 部门规章 / 地方政府规章多源抓取
- 后端 Spring Boot 3 主类 + 端口 8083
- Web 端 Vue 3 + Element Plus 骨架（端口 3003）
- 小程序端 uni-app 骨架

**未完成**（留给 Phase 1+）：
- 各业务 Controller 的真实 Service / Mapper 调用
- AI 模型接入
- Neo4j Driver 接入
- Redis 缓存

---

## Phase 1 - 地方政府规章子系统

**目标**：把需求文档中"地方政府规章"范围内的所有功能模块做成可演示的端到端 demo。

**子任务**：

### 1.1 立法项目全流程
- [ ] `LegislativeProjectController` 接入 `LegislativeProjectServiceImpl`（已完成）
- [ ] 前端 `views/project/index.vue`：列表 + 详情（流程时间轴）
- [ ] 期限预警：列表卡片显示 7 天内到期 / 已逾期
- [ ] 流程可视化：使用 ECharts 绘制流程图

### 1.2 草案生成（基础版，不接 LLM）
- [ ] `DraftController.generate()` 接受 `{ projectId, sourceRegulationId, prompt }`
- [ ] 走**模板拼接**的简化生成（按章节标题 + 上位法条款组装）
- [ ] 落库到 `legislative_draft`，并触发 `draft_version_history`
- [ ] 前端 `views/draft/index.vue`：表单 + 结果预览

### 1.3 智慧审查（基础版）
- [ ] 复用 `government_edition/QualityRule` 的 6 类规则（SUPERIOR_CONFLICT / OVER_POWER / OUTDATED_REF / DUPLICATE / FORMAT / VERBOSE）
- [ ] `ReviewController.submit()` 异步执行规则 → `review_issue` 落库
- [ ] 前端 `views/review/index.vue`：问题列表 + 严重程度颜色标签

### 1.4 智能清理（手动触发）
- [ ] `CleanupController.create()` 支持 3 种类型：DAILY / PERIODIC / THEMATIC
- [ ] `CleanupController.affected()` 返回候选法规列表
- [ ] `CleanupController.suggest()` 给出 KEEP / MODIFY / OBSOLETE 三档建议
- [ ] 前端 `views/cleanup/index.vue`：任务列表 + 决策看板

### 1.5 实施评估（图表）
- [ ] 复用 `evaluation_indicator` 种子数据
- [ ] `EvaluationController.chart()` 返回 ECharts 友好数据
- [ ] 前端 `views/evaluation/index.vue`：雷达图（3 维度） + 折线图（同期对比）

### 1.6 意见征集（基础版）
- [ ] `ConsultationController.create()` 公告发布
- [ ] `submitOpinion()` 接受意见 + 自动归类
- [ ] 简化 AI 归类：先按关键词匹配 → 后期换 LLM
- [ ] 前端 `views/consultation/index.vue`：列表 + 详情 + 分类统计

### 1.7 立法资料库（搜索 + 收藏）
- [ ] `LibraryController.search()` 关键词 + 类型筛选
- [ ] `LibraryController.favorite()` 收藏
- [ ] 前端 `views/library/index.vue`：搜索框 + 列表 + 详情 + 批注

### 1.8 立法信息展示
- [ ] `InfoController.news()` 简版（直接复用 government_edition 的 `quality_issue` 概念）
- [ ] 订阅接口
- [ ] 前端 `views/info/index.vue`：动态列表 + 仪表盘

**里程碑 1**：在地方政府规章场景下，**走通一个项目**从立项 → 起草 → 审查 → 评估 → 资料归档的完整闭环。

---

## Phase 2 - AI 流水线 + 知识图谱全量

### 2.1 LLM 接入
- [ ] 选型：Qwen-long（中文长文）或文心一言
- [ ] `DraftGenerationService`：基于"上位法条文 + 起草说明 + 异地规章片段"做 RAG
- [ ] `ReviewService` LLM 增强：自动判定 + 严重程度

### 2.2 知识图谱
- [ ] `RegulationGraphService` 接入 Neo4j Driver
- [ ] 上下位法关系自动提取（来自 `regulation_relation`）
- [ ] 前端可视化：用 vis.js 或 ECharts graph

### 2.3 评估 BI
- [ ] BI 报表自动生成（按 ECharts 多图组合）
- [ ] 同期对比 + 同类法规对比

**里程碑 2**：用 AI 把"草案生成""智慧审查""评估报告"做到人工校对 5 分钟内可发布。

---

## Phase 3 - 部门规章 + 行政法规扩展

复用 Phase 1，主要差异：
- 部门规章：流程增加"部务会议审议"节点
- 行政法规：流程增加"国务院常务会议审议"

`legislative_stage_template` 已经预留 type 列，仅需补阶段 seed 数据。

---

## Phase 4 - 政民互动

- 意见去重：SimHash + Embedding 双路
- 自动归类：LLM 抽取 5~8 个主题
- 报告自动生成：意见汇总 + 决策建议
- 公众门户：独立前端入口（可考虑小程序端扩展）

---

## 附：与 CaseGuardian 其他子版的关系

| 子版 | 主要复用 | 改造点 |
|------|---------|--------|
| `government_edition` | Spring Boot 骨架 / 实体风格 / Web 端 UI | 包名 + 业务实体 + 流程引擎 + 端口 |
| `university_edition` | uni-app 小程序 / Element Plus 组件 | 业务页面 + pages.json |
| `crawler` | BaseCrawler / 数据库写入模块 | 新增 legislation_spider |
| `database` 目录 | `legal_gov` 的 DDL 风格 | 新建 `legal_legislation` 库 |

立法版保持"三件套 + 一库 + 一爬虫"的结构与 gov-edition 完全平行，方便横向扩缩。
