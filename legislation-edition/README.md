# 智立法 · 行政立法智能辅助平台（legislation_edition）

> 基于 `CaseGuardian` 现有 `government_edition` 三件套（backend / web_frontend / miniprogram）结构的**立法版**子项目。
> 完整需求参见仓库根目录 `行政立法智能辅助平台.md`。

---

## 1. 落地建议（来自需求文档 8.6）

| # | 建议 | 本仓库落地 |
|---|------|----------|
| 1 | 不新建仓库，沿用 `government_edition` 的 `backend / web_frontend / miniprogram` | ✅ `legislation_edition/backend` `web_frontend` `miniprogram` 三件套 |
| 2 | 优先复用 `LegalRegulation`、Neo4j `graph`、`QualityRule`、`CaseFlowService`、`DocumentAutoGenerateController`、`crawler` | ✅ 后端包名 `com.legal.legislation` 复用 gov-edition 技术栈；crawler 新增 `legislation_spider.py` |
| 3 | 重点新建 5 类新业务实体 + 草案生成 AI 流水线 + 三大新工作台 | ✅ 24 张表 + `LegislativeFlowService` + `RegulationGraphService` + 9 个 Controller |
| 4 | 先做拆分，建议先选**地方政府规章** | ✅ `legislative_stage_template` seed 已包含 `LOCAL_RULE` 流程模板 |
| 5 | 流程上把《行政法规制定程序条例》《规章制定程序条例》做成可配置数据 + 派生 `LegislativeFlowService` | ✅ `LegislativeStageTemplate` 表 + `LegislativeFlowServiceImpl.initializeStages` |
| 6 | AI 模型选型：RAG + LLM + ECharts | ⏳ AI 流水线在 Phase 2 |

---

## 2. 目录结构

```
legislation_edition/
├── backend/                       # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/legal/legislation/
│       │   ├── LegislationApplication.java
│       │   ├── common/Result.java
│       │   ├── config/  (SecurityConfig / MybatisPlusConfig)
│       │   ├── controller/  (10 个 Controller)
│       │   ├── entity/  (24 张表对应实体)
│       │   ├── graph/  (RegulationGraphService - Neo4j 子图)
│       │   ├── mapper/  (24 个 Mapper)
│       │   ├── service/  + impl/  (LegislativeFlowService 等)
│       │   └── task/  (DeadlineReminderTask)
│       └── resources/application.yml
├── web_frontend/                  # Vue 3 + Element Plus Web 端
│   ├── package.json / vite.config.js
│   └── src/  (main.js / App.vue / router / api / views / styles)
├── miniprogram/                   # uni-app 微信小程序
│   ├── package.json / project.config.json
│   └── src/  (App.vue / pages.json / 11 个页面)
└── docs/                          # 阶段开发指南 / API 文档
```

---

## 3. 与 `government_edition` 的关系

| 维度 | government_edition | legislation_edition |
|------|-------------------|---------------------|
| 业务对象 | 案件（检察 / 法院 / 行政） | 立法项目 / 法规 / 草案 |
| 主实体 | `CaseInfo` `CaseStage` `CaseAsset` | `LegislativeProject` `LegislativeStage` `Regulation` `LegislativeDraft` |
| 流程引擎 | `CaseFlowService`（按案件类型） | `LegislativeFlowService`（按法规类型，**可配置模板**） |
| 知识图谱 | 案件-法规-证据图 | 法规上下位 / 引用 / 替代关系图 |
| 文档 | 案件文书 / 卷宗 | 立法草案 / 评估报告 / 资料 |
| AI 应用 | 文书校对 / 智能阅卷 | 草案生成 / 智慧审查 / 评估报告 |
| 数据库 | `legal_gov`（端口 8080） | `legal_legislation`（端口 8083） |
| Web 端口 | 3000 | 3003 |

技术栈一致：Spring Boot 3.1.6 / Java 17 / MyBatis-Plus 3.5.8 / MySQL 8.0 / Neo4j 5.x / Spring Security + JWT / Vue 3 + Element Plus / uni-app。

---

## 4. 快速开始

### 4.1 数据库初始化

```bash
mysql -u root -p < legislation_edition/backend/sql/legislation_schema.sql
```

执行后会创建 `legal_legislation` 库 + 24 张表 + 三类流程模板（行政法规 / 部门规章 / 地方政府规章）+ 8 个评估指标 + 6 个审查规则 + 10 个常用资料标签。

### 4.2 后端

```bash
cd legislation_edition/backend
mvn spring-boot:run
# 监听 http://localhost:8083/api
```

### 4.3 Web 端

```bash
cd legislation_edition/web_frontend
npm install
npm run dev
# 访问 http://localhost:3003
```

### 4.4 小程序

```bash
cd legislation_edition/miniprogram
npm install
# 用 HBuilderX 打开后运行到微信开发者工具
```

### 4.5 抓取立法资料

```bash
cd crawler
python main.py --crawl-legislation
# 抓取目标：行政法规（gov.cn）+ 部门规章（moj.gov.cn）+ 地方政府规章（江苏/浙江/北京）
```

---

## 5. 阶段开发指南

| 阶段 | 范围 | 状态 | 关键交付 |
|------|------|------|---------|
| **Phase 0** | 目录骨架 + 实体 + 流程模板 + 立法爬虫 | ✅ 当前 | 24 张表 / LegislativeFlowService / RegulationGraphService / legislation_spider |
| **Phase 1** | 地方政府规章子系统 | ⏳ 下一里程碑 | 完整 CRUD + 草案生成 demo + 清理任务 demo + 评估图表 |
| **Phase 2** | AI 流水线 + 知识图谱全量 | 🔒 待 LLM 接入 | 草案 RAG 生成 / 智慧审查自动判定 / 图谱可视化 |
| **Phase 3** | 部门规章 + 行政法规扩展 | 🔒 后续 | 复用 Phase 1 + 完善 2 套流程模板 |
| **Phase 4** | 政民互动（意见征集门户） | 🔒 后续 | 意见去重 / 自动归类 / 报告自动生成 |

详细计划参见 `docs/ROADMAP.md`。

---

## 6. API 路由速查

| 模块 | 路由前缀 | Controller |
|------|----------|-----------|
| 一 立法项目 | `/legislative-project` | `LegislativeProjectController` |
| 二 草案生成 | `/draft` | `DraftController` |
| 三 智慧审查 | `/review` | `ReviewController` |
| 四 智能清理 | `/cleanup` `/regulation` | `CleanupController` `RegulationController` |
| 五 实施评估 | `/evaluation` | `EvaluationController` |
| 六 意见征集 | `/consultation` | `ConsultationController` |
| 七 资料库    | `/library` | `LibraryController` |
| 八 信息展示  | `/info` | `InfoController` |
| 认证     | `/auth` | `AuthController` |

详细 API 草图见需求文档 `行政立法智能辅助平台.md` 第三章。

---

## 7. 数据表清单（24 张）

按模块分组：

- **模块一**：legislative_project / legislative_stage / legislative_deadline / legislative_stage_template
- **模块二**：legislative_draft / draft_version_history
- **模块三**：review_record / review_issue / review_rule
- **模块四**：cleanup_task / cleanup_suggestion
- **模块五**：evaluation_task / evaluation_indicator / evaluation_result
- **模块六**：consultation / opinion / opinion_reply / opinion_category
- **模块七**：library_material / library_tag / material_tag / material_note
- **共用**：regulation / regulation_relation

ER 图见 `行政立法智能辅助平台.md` 第三章 8 个 mermaid 图。

---

## 8. 已知 TODO（按优先级）

- [P0] `AuthController` 接入 JWT（当前是占位）
- [P0] `SecurityConfig` 收紧权限（当前全部 permitAll）
- [P0] 各业务 Controller 接入真实 Service / Mapper
- [P1] `RegulationGraphService` 接入 Neo4j Driver（当前是内存版）
- [P1] `DraftController` 接入 LLM（RAG + 异地规章库）
- [P1] `CleanupController` 接入 CleanupService（清理规则引擎）
- [P1] `EvaluationController` 接入指标计算 / 图表数据接口
- [P2] Neo4j 真实环境部署（参考 `application.yml` 中注释项）
- [P2] Redis 缓存层（gov-edition 已注释，可统一开）

---

## 9. 联系人 / 责任分工

立法版由原 `CaseGuardian` 团队承接，重点维护 `legislation_edition/` 目录；与 `government_edition / university_edition` 平行演进。
