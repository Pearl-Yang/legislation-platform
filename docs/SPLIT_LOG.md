# 拆分历史 (Split Log)

## 2026-10-03: 从 CaseGuardian 拆分

**动机**: `CaseGuardian` 仓库承载了多个业务方向(政府案件 / 高校 / 立法),
虽然代码层面没有相互依赖,但语义上不同业务、不应共用代码仓库。

立法平台已具备独立的业务完整度(8 大模块全部贯通),
可以独立交付与部署,故做物理拆分。

### 拆分操作

| 项目 | CaseGuardian | legislation-platform |
|---|---|---|
| 立法后端 + 前端 + 小程序 + 资料 | `legislation_edition/` | `legislation-edition/` |
| 行政立法法规爬虫 | `crawler/` | `crawler/` |

**保留在 CaseGuardian 的目录**:
- `government_edition/`(政府案件管理)
- `backend/`(原统一后端)
- `database/`、`docs/`、`pom.xml`、`package.json` 等基础设施

### 拆分前的准备 (CaseGuardian 仓内的 commit)

1. `chore(legislation): 拆分前清理 - 移除与 government_edition 的文字关联`
   - 清理 `Result.java`、`DeadlineReminderTask.java` 注释中对 `government_edition` 的文字提及
   - 仅注释修改,无任何代码行为变更

### 拆分后的清理 (在 CaseGuardian 仓内的 commit)

- 后续 commit: `chore(repo): 移除已迁移到 legislation-platform 的目录`
  - 物理 `git rm legislation_edition crawler`

### 校验

- ✅ `legislation-edition/backend` 内的 `com.legal.legislation.*` 包名完全独立
- ✅ 跨项目代码引用扫描:0 处实际 import / 0 处 maven dependency
- ✅ 文档类提及(README / ROADMAP)在拆分后已替换为指向新仓库的链接
- ✅ `mvn clean compile` 一次通过(93 源文件,0 错误 0 警告)

### 后续注意点

1. **数据库 schema** 仍用 `legislation_schema.sql`,与原库同源
2. **认证方式**:开发期 `X-User-Id` 头简化版,生产请补 JWT / OAuth2
3. **AI 占位**:Draft/Review/Cleanup/Evaluation 中的 AI 逻辑当前为规则引擎 + 关键词占位,
   接入真实 LLM 时优先替换 `application.yml` 中 `ai.draft-generation.enabled` 开关
4. **Neo4j 图谱**:暂未启用,后续启用时把 `application.yml` 中 `spring.neo4j.*` 解开
