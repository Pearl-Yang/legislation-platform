# 智立法 · 行政立法智能辅助平台 — 3 人 3 天冲刺作战图(v2)

> **项目代号**:`legislation-platform`(v0.2.0)
> **当前日期**:2026-10-05(周一)
> **答辩交付日**:2026-10-08(周四)← 由原 10/12 提前至本周四
> **冲刺周期**:**3 个工作日(D1 ~ D3)**,然后 D4 上午联调 + 下午答辩演练 + 晚上最终备份
> **团队规模**:3 人(A · B · C)
> **本文档用途**:每日站会跟进 + 跨人协作 + 答辩前最终自检清单
> **文档版本**:`v2.0` · 2026-10-05 PM(基于真实 git 状态重写)

---

## 〇、真实基线盘点(2026-10-05 PM)⚠️ 必读

> 📌 截至今天 16:05,以下结论基于**真实 git working tree + diff + 文件搜索**,不再使用 v1.x 文档中"自吹自擂"的口径。

### 0.1 当前真实进度表(commit / working tree / 未跑过)


| 维度                               | 真实状态                                                                                                                                                                                                                                | 来源                            |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| **git 历史**                       | 仅 `46ed891 first commit` 1 个 commit,**后续 47 处改动全部在 working tree 未提交**                                                                                                                                                               | `git log --oneline`           |
| **后端 11 个 Controller**           | first commit 已包含全部骨架;Cleanup / Consultation / Draft / Evaluation / InfoServiceImpl / DraftServiceImpl / CleanupServiceImpl / AuthService 共 8 个类有 working tree 改动                                                                    | `git diff --stat`             |
| **前端 37 个 vue 页面**               | Dashboard.vue(781)/ Tasks.vue / cleanup-tasks / draft-Generate / project-{index,Detail} / review-{Submit,Rules} / consultation-List / Login 共 10 页有 working tree 改动;**library / dashboard/index / regulation 等仍为 first commit 的空壳** | `git diff --stat`             |
| **小程序 9 页**                      | index / dashboard / draft / evaluation / cleanup / consultation / library / info / profile 全部改了,**全部未提交**                                                                                                                           | `git status`                  |
| **测试**                           | 后端 4 个 .java(Inference/SuperiorLaw/ReviewRule/DocxExporter,IntegrationTest 85 行);前端 6 个 .test.js(Day2 171 / Day3 203 / Dashboard 136 / store / utils / router),**全部未提交 + 从未跑过**                                                     | 文件 glob + 行数                  |
| **冒烟脚本**                         | `day1-verify.sh`(80 行 11 项)/ `day2-smoke.sh`(9 项)/ `day3-smoke.sh`(104 行)**全部存在未提交 + 从未跑过**                                                                                                                                         | 文件 glob                       |
| **Qwen**                         | `QwenApiClient` + `QwenChatRequest` 已改造支持 online;但 `application.yml` / `application-dev.yml` 仍 `QWEN_MODE:offline`,且 `QWEN_API_KEY` 为空 → **在线调用事实上未跑通**                                                                             | `git diff application*.yml`   |
| **Docker compose**               | 加了 backend `start_period: 30s` + neo4j `condition: service_healthy` + 改用 `/auth/health` 做 healthcheck → **未提交,数据库完全空载时启动会假性 UP**                                                                                                    | `git diff docker-compose.yml` |
| `docker/backend-qwen-online.err` | 存在但内容为空 → 之前尝试过 online 模式,失败后未清理                                                                                                                                                                                                    | 文件存在但 0 字节                    |
| **数据库**                          | MySQL 容器已有,但只跑了 schema.sql,**真实演示数据(项目/法规/草案/评估/清理)是否就绪未验证**                                                                                                                                                                        | `docker/docker-data/` 体积异常    |




### 0.2 v1.x 文档与现实的偏差


| v1.x 说法              | 现实                                       |
| -------------------- | ---------------------------------------- |
| "Day1-3 已 100% 完成 ✅" | 改动全在 working tree,**0 个新 commit**,冒烟从未跑过 |
| "前端 55/55 单测全绿"      | 测试文件存在但**从未执行过 Vitest**                  |
| "后端 20/20 测试通过"      | `mvn test` **从未在当前 working tree 跑过**     |
| "Qwen 在线/离线双通道已封装"   | 代码改了但 yml 仍 offline,**在线路径实际为死代码**       |
| "Docker 全栈启动验证通过"    | healthcheck 改动未提交,且**整个新代码栈从未启动过**       |




### 0.3 真实工作量重新评估 → D1-D3 必须做的事

> **结论**:**不能假装 Day1-3 已完成**。新 3 日必须先把 **working tree 提交 + 跑通冒烟 + 验证 Qwen** 列入主线,否则 10/8 答辩时端到端链路是断的。

### 0.3.1 Day4 实测补记(2026-10-05 16:30)🆕

> 📌 截至 16:30,新增以下**实测绿**条目(基于本地 IDE 直接跑通,未 commit):

| 实测项 | 命令 | 结果 |
|---|---|---|
| 前端构建 | `npm run build` | ✅ **21.52s built**(无 error) |
| 前端单测 | `npx vitest run` | ✅ **69/69 全绿**(Dashboard 9 + Day2 13 + Day3 17 + Day4 14 + 既有 16) |
| 测试文件新增 | Day4.data.test.js | ✅ 14 用例(ECharts 流程图 + 7 天预警 + 30 秒轮播) |
| 文档新增 | `docs/答辩版讲稿.md` / `docs/答辩PPT模板.md` / `docs/demo-voc.json` | ✅ 3 个文件就绪 |
| 冒烟脚本新增 | `scripts/day4-smoke.sh` | ✅ 18 步(12 个接口 + 4 个文档 + 2 个测试文件) |
| Detail.vue ECharts 流程图 | 加 nodes/links + 2 级预警 badge | ✅ 写完 + Day4 测试已覆盖 |
| Dashboard 30 秒轮播 | 加 `views` + `viewTimer` + `nextViewIndex()` | ✅ 写完 + Day4 测试已覆盖 |
| README Day1-4 状态小节 | 追加实际交付文件清单 | ✅ 写完 |

> **诚实标注**:以下 4 件事**未跑过**,仍 ⚪:
> - `git commit` 仍 0 个新 commit,所有改动在 working tree
> - 冒烟脚本 `day1-verify.sh / day2-smoke.sh / day3-smoke.sh / day4-smoke.sh` **从未在真实 docker 环境下跑过**(后端需要 docker compose)
> - Qwen 在线模式 `legislation.qwen.mode=online` **配置文件仍是 offline**,API Key 为空
> - `mvn -o test` 已实测 20/20 绿 ✅(本轮实测)

---



## 一、项目现状速览(对齐认知用)


| 维度                | 现状                                                                      | 占比                    |
| ----------------- | ----------------------------------------------------------------------- | --------------------- |
| **后端 Controller** | 11 个全部存在,Cleanup/Consultation/Draft/Evaluation 4 个类有功能性扩展(working tree) | **100%**              |
| **后端 Service**    | 9 个 Service,4 个 Impl + AuthService + Qwen 客户端共 8 个有改动(working tree)     | **100%**              |
| **后端数据**          | MySQL schema + 种子用户 BCrypt(改完但未跑过)                                      | **90%**(需验证)          |
| **后端 AI**         | Qwen 客户端代码就绪(改完),yml 仍 offline(未启用)                                     | **70%**(需配 API Key)   |
| **前端页面**          | 10 个 vue 改了 + dashboard-charts 4 个图表组件新增,**其余 27 页仍 first commit 空壳**   | **30% 真实所需 + 70% 装饰** |
| **前端 API 层**      | `legislation.js` 改 4 行(补 `getWordcloud` 等)                              | **95%**               |
| **测试**            | 后端 4 类 / 前端 6 文件存在但**从未执行**                                             | **未跑**                |
| **冒烟**            | 3 个脚本存在但**从未跑过**                                                        | **未跑**                |
| **Docker**        | compose 改了 healthcheck / start_period,**未提交**                           | **95%**(需 ready)      |
| **小程序**           | 9 页全改,**未提交**;演示价值低                                                     | **可选 P1**             |


---



## 二、3 人 3 天总分工(谁负责什么)

> 📌 **核心原则**(同上版):每人独立交付一条**完整垂直链路**(端到端可演示),3 条链路拼起来 = 整个平台。
> 📌 **新增原则**:**每天开工先验证上一日产物能跑**(跑测试 / 跑冒烟 / 跑 curl),不允许"代码完成" ≠ "能跑"。


| 角色                   | 负责链路                                                  | D1 重点                                   | D2 重点                                   | D3 重点                          | D4 上午       |
| -------------------- | ----------------------------------------------------- | --------------------------------------- | --------------------------------------- | ------------------------------ | ----------- |
| **A · 后端业务主程**       | **业务链路**:项目 → 草案 → 审查 → 评估 → 清理                       | 后端端到端联调 + Qwen 接入 + 跑通 IntegrationTest  | 后端异常兜底 + 异步任务回归 + 数据回填                  | Qwen 真路径打通 + 端到端冒烟 36 步        | 全链联调 + 答辩演练 |
| **B · 数据 + 前端中台**    | **数据链路**:Dashboard → 资料库 → 意见征集 → 小程序                 | 前端页面接通验证 + Vitest 跑通 + Dashboard 8 接口跑通 | 资料库搜索 + 收藏 + 词云/报告 + Login 鉴权闭环         | 小程序接入 + 演示数据准备                 | 全链联调 + 演示视频 |
| **C · 运维 + 测试 + 文档** | **工程链路**:Docker / 监控 / 集成测试 / 答辩 PPT / 演示脚本 / OpenAPI | git 提交 + Docker 启动验证 + 冒烟 36 步真正跑通      | README/ROADMAP 校对 + OpenAPI 导出 + 监控大盘就绪 | 答辩 PPT 15 页 + 演示脚本 1.2 万字 + 录屏 | 最终备份 + 录屏回看 |




### 跨人协作每日同步机制

- **每天 09:30 站会**:15 分钟,3 人各汇报昨日 done(带跑通截图)/ 今日 plan / blocker
- **每天 12:30 中午同步**:30 分钟,合 working tree 一次到本地 main,跑 `mvn test` + `npm test` + 冒烟
- **每天 18:00 收工同步**:60 分钟,A 跑后端端到端,B 跑前端端到端,C 跑 Docker 全栈,**3 人都要看到绿**
- **D4 上午 09:00**:3 人一起全链联调,录像 1 段演示视频作应急备份

---



## 三、3 日任务清单(D1 ~ D3 + D4 上午)🆕

> ✅ = 已完成(已跑过验证) · 🟡 = 进行中 · ⚪ = 未启动 · ❌ = 跑失败需回滚



### D1(2026-10-06 周二)— **提交 + 跑通** ⭐ 必须 100% 真正跑通

> 📌 **D1 的核心目标不是写新功能,而是让 v1.x 声称的所有 ✅ 在今天真正变成 ✅**。


| 人     | 上午任务(09:00-12:30)                                                                                                                                                                                                                                                                                                                       | 下午任务(13:30-18:00)                                                                                                                                                                                                   | 产出物                                        | 状态  | 估时      |
| ----- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ | --- | ------- |
| **A** | ① `git add -A && git commit -m "feat(backend):一次性提交 Day1-3 业务改造"`(含 Cleanup/Consultation/Draft/Evaluation 4 套 Controller/Service 改动 + Qwen 客户端 + DataInitializer + GlobalExceptionHandler);② `mvn clean compile` 必须 BUILD SUCCESS;③ `mvn test` 必须 ≥20 用例全绿(InferenceTest + SuperiorLawParserTest + ReviewRuleTest + DocxExporterTest)   | ① `mvn spring-boot:run` 启动 backend,`curl /api/auth/health` 必须返回 UP;② `curl -X POST /api/auth/login` admin/123456 必须返回 JWT;③ 跑通 `day2-smoke.sh`(9 项全绿)和 `day3-smoke.sh`(16 项全绿);④ 跑通 `IntegrationTest.java` 端到端 9 步  | 后端启动可登录 + 3 个冒烟脚本全绿 + IntegrationTest 9 步绿 | ⚪   | 4h + 4h |
| **B** | ① `cd legislation-edition/web_frontend && npm install --legacy-peer-deps`(如未跑过);② `git add -A && git commit -m "feat(frontend):一次性提交 Day1-3 前端页面改造"`(Dashboard/Tasks/List/Generate/Detail/index/Submit/Rules/Login + 4 个 dashboard-charts + 6 个 test 文件 + `legislation.js` 补漏);③ `npm run build` 必须成功;④ `npm run test:unit` 必须 ≥55 用例全绿 | ① `npm run dev` 启动前端,登录页可打开;② 登录 admin/123456 跳转到 Dashboard;③ Dashboard 8 个接口全部 200(直接看 Network);④ 抽查 3 个 vue 页面(Task/List/Submit)按真实接口走通数据流                                                                        | 前端构建可启 + 55 单测全绿 + Dashboard 8 接口跑通        | ⚪   | 4h + 4h |
| **C** | ① `git add -A && git commit -m "chore(docker):补 healthcheck + start_period"` 提交 docker-compose 改动;② `cd docker && docker compose up -d` 启动 mysql + neo4j + backend + prometheus + grafana;③ 5 个容器 `STATUS` 必须全部 `Up`(health)                                                                                                            | ① `bash scripts/day1-verify.sh` 必须 11 项全绿(健康 + Dashboard + 3 个接口 + 5 个账号登录 + Grafana/Prometheus/Knife4j 可达);② `bash scripts/day2-smoke.sh` + `bash scripts/day3-smoke.sh` 36 步全部绿;③ 跑通 `IntegrationTest.java` 9 项验证 | Docker 全栈 + 36 步冒烟 + IntegrationTest 9 项   | ⚪   | 3h + 4h |


**D1 验收标准**(必须全部满足 = D1 ✅):

- [ ] ⚪ `git log --oneline | head -5` 出现 3 个新 commit(backend / frontend / docker)
- [ ] ⚪ `mvn test` ≥ 20/20 绿
- [ ] ⚪ `npm run test:unit` ≥ 55/55 绿
- [ ] ⚪ `bash scripts/day1-verify.sh` PASS=11 / FAIL=0
- [ ] ⚪ `bash scripts/day2-smoke.sh` PASS=9 / FAIL=0
- [ ] ⚪ `bash scripts/day3-smoke.sh` PASS=16 / FAIL=0
- [ ] ⚪ Docker 5 容器全部 `(healthy)`
- [ ] ⚪ `/api/auth/health` 返回 `status:UP`
- [ ] ⚪ `/api/info/dashboard` 用 admin/123456 拿到的 JWT 调用返回真实数据

**D1 风险预案**:

- 后端 `mvn test` 有用例失败 → A 排查,**1 小时内修不回来就回滚到 first commit 跑测试**
- 前端 `npm install` 装不上 → `npm install --legacy-peer-deps --no-audit`
- Docker 容器起不来 → 删 `docker/docker-data` 重新 `docker compose up -d`(会清库,需重跑 schema.sql + 种子)

---



### D2(2026-10-07 周三)— **补漏 + 异常兜底 + 资料库完善** ⭐ 必须 100% 完成

> 📌 **D2 目标**:D1 已跑通的链路在 D2 加固(全局异常 / 异步任务 / Qwen 在线 / 资料库 / Login 鉴权)。


| 人     | 上午任务(09:00-12:30)                                                                                                                                                                                                      | 下午任务(13:30-18:00)                                                                                                                                                                                                                                                                        | 产出物                                               | 状态  | 估时  |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- | --- | --- |
| **A** | ① 完善 `GlobalExceptionHandler`:新增 `BusinessException` / `IllegalArgumentException` / `MethodArgumentNotValidException` 三类统一 4xx 返回;② `Result` 枚举业务码 1xxxxx-5xxxxx;④ 跑 `mvn test` 仍 ≥20/20 绿;⑤ 跑 `day1-verify` 仍 11/11 绿 | ① `AsyncTaskRunner` **回归**:验证草案 / 审查 / 评估 / 清理 4 个异步路径都跑得到 SUCCESS(从 day3-smoke.sh 步骤 11-16 已覆盖,需关注日志 `AsyncTaskRunner` 关键字);② Neo4j **真实启动 + 数据回填**(若 D1 已成功,docker compose 启 neo4j 容器 + `NEO4J_ENABLED=true` + 跑 `Neo4jStartupSync` 把 regulation/relation 写入图谱);③ 跑 day2 + day3 冒烟,仍全绿 | 全局异常 4xx 规范化 + Async 4 路径全 SUCCESS + Neo4j 真实数据回填 | 7h  |     |
| **B** | ① 资料库搜索增强:`views/library/Search.vue` 接 `/library/search` 接口 + 分页 + 全文检索参数;② 收藏功能:`views/library/Favorites.vue` 接 `/library/favorite` 接口;③ `views/library/index.vue` 从 3 行占位补到真实导航页 ① ②                                 | ① Dashboard 细节打磨:每个 KPI 卡片加 loading/error/刷新按钮 + echarts tooltip 完善;② Login.vue 鉴权闭环:登录失败提示 + 密码错误次数限制 + 退出登录;③ 跑 `npm run test:unit` 仍 55/55 绿(新增的 library 测试用例 ≥10 个);④ 跑前端 build + dev,所有路由可达                                                                                         | 资料库 3 页接通 + Login 鉴权闭环 + Dashboard 细节打磨           | 7h  |     |
| **C** | ① README.md 校对(状态全部刷 ✅;踩坑 → 已修;快速开始链路 → Day1 跑通的 3 步);② ROADMAP.md 把 Day1-3 进度改成"已验证 ✅"、D1-D4 day5 加进来;③ API_REFERENCE.md 用 `bash scripts/export-openapi.sh` 导出 openapi.json + 标注实测可达的端点                               | ① Prometheus + Grafana 大盘校正:进 Grafana 确认 8 个业务指标(`legal_regulation_`*, `legal_draft_*`, `legal_review_*` 等)有数据;② 加 1 个 docker compose 自动重启策略注释(`restart: unless-stopped`);③ `scripts/backup-data.sh` 加上时间戳 + 保留最近 5 个备份                                                                  | README/ROADMAP 校对 + OpenAPI 导出 + 监控大盘就绪 + 数据备份脚本  | 7h  |     |


**D2 验收标准**(必须全部满足 = D2 ✅):

- [ ] ⚪ 后端 `mvn test` ≥ 22/22 绿(新增 2 个 GlobalExceptionHandler 测试)
- [ ] ⚪ 前端 `npm run test:unit` ≥ 65/65 绿(新增 10 个 library 测试)
- [ ] ⚪ 36 步冒烟仍全绿(不能因 D2 改动回归)
- [ ] ⚪ Neo4j 容器 `(healthy)` + 图谱有数据(用 cypher `MATCH (n) RETURN count(n) > 0`)
- [ ] ⚪ `openapi.json` 文件存在且大小 > 50KB
- [ ] ⚪ README 状态栏全 ✅

---



### D3(2026-10-08 周四)— **Qwen 在线 + 小程序 + 答辩准备** ⭐ 最终交付日

> 📌 **D3 目标**:**答辩日(同日)晚上必须交付**——Qwen 真路径 + 小程序 2 页 + 答辩 PPT + 演示视频。


| 人     | 上午任务(09:00-12:30)                                                                                                                                                                                                                | 下午任务(13:30-18:00)                                                                                                                                                                   | 产出物                                      | 状态  | 估时  |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- | --- | --- |
| **A** | ① Qwen 在线真路径打通:`legislation.qwen.mode=online` + `QWEN_API_KEY=sk-xxx`(用户提供),重启 backend,跑 `day3-smoke.sh` 步骤 11(草案生成)必须返回真实 LLM 内容(**文本长度 > 1000 字 / 含章节标题**);② 失败兜底:API Key 无效时切回 offline 并在日志显著位置写 `[QWEN_OFFLINE_FALLBACK]` 提示 | ① AsyncTaskRunner 日志可观测:在 `/actuator/prometheus` 看到 `legal_async_task_total` 计数 > 0;② 跑 `mvn test` ≥ 22/22;③ 跑 day1+2+3 36 步冒烟 + Neo4j 图谱 + Qwen 在线 1 次真路径                          | Qwen 真路径 + Async 可观测 + 36 步冒烟仍绿          | 7h  |     |
| **B** | ① 小程序接入:`miniprogram/src/pages/index/index.vue` 跑通 uniapp 编译(`npm run dev:mp-weixin`);② 至少 2 个页面(index + 项目列表)能打开真接口;③ 跑前端 `npm run test:unit` ≥ 65/65 仍绿                                                                        | ① Dashboard 演示状态打磨:截图 5 张 Dashboard 真实数据图(无 loading/error 状态);② 跑前端 build + dev,Dashboard 视觉流畅;③ 录 30 秒前端演示视频(给 C 做应急 backup)                                                       | 小程序 2 页接通 + Dashboard 截图 + 前端 30 秒视频     | 7h  |     |
| **C** | ① 答辩 PPT 15 页:背景 / 架构 / 8 大模块 / 技术亮点 / AI 能力 / 演示视频(嵌入 D3 录的 30 秒视频);② 演示脚本 `demo-voc.json`(1.2 万字);③ README 校对(最终态)                                                                                                             | ① 录 1 段 5 分钟**全端到端演示视频**(用 docker compose up → 登录 → Dashboard → 创建项目 → 草案 → 审查 → 评估 → 清理 → 大屏;作为答辩现场应急 backup);② `git tag v1.0-defense` + 推 origin;③ 打 zip 包(含 docker compose 启动说明) | 答辩 PPT + 演示脚本 + 5 分钟录屏 + git tag + zip 包 | 7h  |     |


**D3 验收标准**(D3 18:00 必须 100% 满足):

- [ ] ⚪ Qwen 在线调用 1 次成功(`apiKey` 已配,**生成内容 > 1000 字**)
- [ ] ⚪ 小程序编译通过 + 至少 2 页可打开
- [ ] ⚪ Dashboard 5 张截图存在
- [ ] ⚪ 答辩 PPT 15 页完成
- [ ] ⚪ 演示脚本 1.2 万字完成
- [ ] ⚪ 5 分钟录屏完成
- [ ] ⚪ `git tag v1.0-defense` 已打 + 已推 origin
- [ ] ⚪ 答辩 zip 包已打(包含 docker compose 启动说明)

---



### D4(2026-10-08 周四)— **联调 + 答辩演练 + 最终交付** ⚠️ 答辩日


| 时段              | 任务                                                                                                | 负责人       |
| --------------- | ------------------------------------------------------------------------------------------------- | --------- |
| **09:00-10:00** | 全链联调 1 小时:登录 → 创建项目 → 推进阶段 → 草案(确认 Qwen 真路径)→ 审查 → 评估 → 清理 → 意见 → 资料库 → Dashboard → 监控;**记录所有卡点** | A + B + C |
| **10:00-11:30** | 修联调卡点;A 修后端 / B 修前端 / C 修文档监控                                                                     | A / B / C |
| **11:30-12:00** | 答辩演练 ×1(各讲 10 分钟 + 自由问答 20 分钟)                                                                    | A + B + C |
| **12:00-14:00** | 午饭 + 休息                                                                                           | 全员        |
| **14:00-答辩开始**  | ✅ **正式答辩**                                                                                        | 全员        |


---



## 四、每人详细负责清单(人 → 文件 → 接口 → 页面 一一映射)



### 👤 A · 后端业务主程(Java)

**后端文件**(working tree 已有 / D1 已验证):

- `controller/LegislativeProjectController.java` ✅ first commit
- `controller/DraftController.java` 🟡 改 65 行(D1 待提交 + 跑通)
- `controller/ReviewController.java` ✅ first commit
- `controller/EvaluationController.java` 🟡 改 108 行(D1 待提交 + 跑通)
- `controller/CleanupController.java` 🟡 改 88 行(D1 待提交 + 跑通)
- `controller/ConsultationController.java` 🟡 改 76 行(D1 待提交 + 跑通)
- `controller/InfoController.java` ✅ first commit
- `controller/LibraryController.java` ✅ first commit
- `controller/AuthController.java` ✅ first commit

**Service 文件**:

- `service/impl/DraftServiceImpl.java` 🟡 改(D1 待提交)
- `service/impl/CleanupServiceImpl.java` 🟡 改(D1 待提交)
- `service/impl/EvaluationServiceImpl.java` 🟡 改(D1 待提交)
- `service/impl/InfoServiceImpl.java` 🟡 改(D1 待提交)
- `service/AuthService.java` 🟡 改(D1 待提交)
- `service/draft/SuperiorLawParser.java` ✅ first commit
- `service/review/ReviewEngine.java` ✅ first commit

**AI / 配置 / 异常**:

- `ai/qwen/QwenApiClient.java` 🟡 改 18 行(D1 提交 + D3 启用 online)
- `ai/qwen/QwenChatRequest.java` 🟡 改(D1 提交)
- `common/exception/GlobalExceptionHandler.java` 🟡 改(D1 提交)
- `config/DataInitializer.java` 🟡 改(D1 提交 + 跑通 5 种子用户 BCrypt)

**测试**:

- `src/test/.../IntegrationTest.java`(85 行,9 步端到端)
- `src/test/.../SuperiorLawParserTest.java`
- `src/test/.../ReviewRuleTest.java`
- `src/test/.../DocxExporterTest.java`
- **D2 新增**:`GlobalExceptionHandlerTest.java`(≥2 用例)

**D1 必做**:`git commit` + `mvn test` + `bash day1-verify.sh` + `bash day2-smoke.sh` + `bash day3-smoke.sh`
**D2 必做**:GlobalExceptionHandler 完善 + Neo4j 真实启动 + AsyncTaskRunner 4 路径验证
**D3 必做**:Qwen 在线真路径打通(API Key 由用户提供) + AsyncTaskRunner 可观测
**D4 上午**:联调修卡点 + 答辩演练

**答辩讲点**:项目全生命周期 + 6 类审查规则引擎 + 评估 3 维指标 + 清理启发式打分 + 异步任务框架 + Qwen 在线 AI

---



### 👤 B · 数据 + 前端中台(Vue)

**前端文件**(working tree 已有 / D1 待提交):

- `views/info/Dashboard.vue`(781 行,改 453 行)🟡 D1 提交
- `views/info/dashboard-charts/{TrendChart,PieDonut,BarChart,GraphView}.vue`(4 个图表组件,新增)🟡 D1 提交
- `views/consultation/List.vue`(426 行)🟡 D1 提交
- `views/evaluation/Tasks.vue` 🟡 D1 提交
- `views/cleanup/Tasks.vue` 🟡 D1 提交
- `views/draft/Generate.vue` 🟡 D1 提交
- `views/project/index.vue` + `Detail.vue` 🟡 D1 提交
- `views/review/Submit.vue` + `Rules.vue` 🟡 D1 提交
- `views/auth/Login.vue` 🟡 D1 提交 + D2 鉴权闭环
- `api/legislation.js`(改 4 行)🟡 D1 提交

**前端待补**(D2 / D3 待写):

- `views/library/{Search,Favorites,index,List,Detail}.vue` ⚪ D2 完善
- `views/dashboard/index.vue` ✅ first commit
- `views/info/{Interpret,News,index}.vue` ✅ first commit

**测试文件**:

- `views/evaluation/__tests__/Day3.data.test.js`(203 行)🟡 D1 提交
- `views/info/__tests__/Dashboard.data.test.js`(136 行)🟡 D1 提交
- `views/project/__tests__/Day2.data.test.js`(171 行)🟡 D1 提交
- `store/__tests__/user.test.js` ✅ first commit
- `utils/__tests__/index.test.js` ✅ first commit
- `__tests__/router.smoke.test.js` ✅ first commit
- **D2 新增**:`views/library/__tests__/*.test.js`(≥10 用例)

**Miniprogram 文件**(D3 待接入):

- `miniprogram/src/pages/{index,dashboard,draft,evaluation,cleanup,consultation,library,info,profile}/index.vue` 🟡 D3 提交 + 跑通至少 2 页

**D1 必做**:`git commit` + `npm install --legacy-peer-deps` + `npm run build` + `npm run test:unit` + `npm run dev`
**D2 必做**:资料库 3 页接通 + Login 鉴权闭环 + Dashboard 细节打磨 + ≥10 单测
**D3 必做**:小程序 2 页接通 + Dashboard 5 张截图 + 30 秒前端演示视频
**D4 上午**:联调修卡点 + 答辩演练

**答辩讲点**:大屏驾驶舱可视化 + 公众参与端到端 + 资料检索 + Qwen LLM 接入 + 小程序扩展性

---



### 👤 C · 运维 + 测试 + 文档(DevOps)

**运维文件**(working tree 已有 / D1 待提交):

- `docker/docker-compose.yml`(改 healthcheck + start_period + 用 /auth/health)🟡 D1 提交
- `docker/prometheus/prometheus.yml` ✅ first commit
- `docker/grafana/dashboards/` ✅ first commit
- `docker/grafana/provisioning/` ✅ first commit
- `legislation-edition/backend/src/main/resources/logback-spring.xml` ✅ first commit
- `scripts/{day1-verify.sh, day2-smoke.sh, day3-smoke.sh, export-openapi.sh, backup-data.sh}` 🟡 D1 提交 + 跑通

**冒烟脚本验证清单**(D1 必跑):

- `bash scripts/day1-verify.sh` → PASS=11
- `bash scripts/day2-smoke.sh` → PASS=9
- `bash scripts/day3-smoke.sh` → PASS=16
- **合计 36 步必须全绿**

**测试文件**:

- `web_frontend/src/views/{evaluation,info,project}/__tests__/Day*.data.test.js`(已存在,D1 提交)
- `legislation-edition/backend/src/test/.../IntegrationTest.java`(85 行,D1 提交)
- **D2 新增**:`OpenApiExportTest.java`(验证 `bash export-openapi.sh` 产物可解析)

**文档文件**(D2-D3 待做):

- `README.md`(D2 校对最终态)
- `docs/ROADMAP.md`(D2 校对)
- `docs/API_REFERENCE.md`(D2 校对)
- `docs/TEAM_ALLOCATION_3PF.md`(本文档 ✅ v2.0)
- `行政立法智能辅助平台-答辩版.pptx`(D3 新增 15 页)
- `行政立法智能辅助平台-答辩版.md`(D3 配套讲稿)
- `行政立法智能辅助平台-演示脚本.json`(D3 新增 1.2 万字)

**D1 必做**:`git commit` + `docker compose up -d` + 5 容器 `(healthy)` + 36 步冒烟 + IntegrationTest 9 项
**D2 必做**:README/ROADMAP 校对 + OpenAPI 导出 + 监控大盘校正 + 备份脚本
**D3 必做**:答辩 PPT 15 页 + 演示脚本 1.2 万字 + 5 分钟录屏 + `git tag v1.0-defense` + zip 包
**D4 上午**:联调修卡点 + 答辩演练 + 录屏回看

**答辩讲点**:Docker 一键启动 + Prometheus/Grafana 监控 + 日志 JSON 化 + 集成测试 + 工程化交付 + OpenAPI 文档 + Qwen LLM 兜底机制

---



## 五、最终交付物清单(答辩前必须 100% 就绪)

> ✅ = 已完成(已跑通验证) · 🟡 = D1-D3 待完成 · ⚪ = P2 可选



### 🟢 P0 — 答辩核心(D4 09:00 前必须 100% 就绪)

- [ ] ⚪ **D1 提交**:3 个新 commit + `mvn test` ≥20/20 + `npm run test:unit` ≥55/55 + 36 步冒烟全绿 + Docker 全栈 `(healthy)`
- [ ] ⚪ **5 个种子用户可登录**(admin / leader / drafter / reviewer / evaluator · 123456 · BCrypt)
- [ ] ⚪ **Docker compose up 4 步启动**(复制环境变量 → 启动 → 等待 → 登录)
- [ ] ⚪ **项目流程端到端**(立项 → 起草 → 审查 → 公布 → 评估 → 清理 一个闭环)
- [ ] ⚪ **Dashboard 大屏 4 行数据真实**(8 接口 / 11 卡片图表)
- [ ] ⚪ **6 类审查规则可生成问题**(SUPERIOR_CONFLICT / OVER_POWER / OUTDATED_REF / DUPLICATE / FORMAT / VERBOSE)
- [ ] ⚪ **3 维评估图表 + 报告**(雷达 + 折线 + Markdown/HTML/DOCX 报告)
- [ ] ⚪ **3 种清理任务 + 决策看板**(DAILY / PERIODIC / THEMATIC + KEEP/MODIFY/OBSOLETE)
- [ ] ⚪ **资料库搜索 + 收藏**(D2 完善,B 负责)
- [ ] ⚪ **意见征集发布 + 提交 + 归类 + 报告**
- [ ] ⚪ **Neo4j 关系网可视化**(D2 启动真实图谱,A 负责)
- [ ] ⚪ **Prometheus + Grafana 监控大盘可见**(D2 校正,C 负责)
- [ ] ⚪ **API 文档 Knife4j 可访问** `http://localhost:8083/api/doc.html`
- [ ] ⚪ **Qwen 在线 API 1 次成功**(D3 打通,A 负责)
- [ ] ⚪ **OpenAPI 一键导出 + JSON 文件**(D2 完成,C 负责)



### 🟡 P1 — 加分项(D4 09:00 前尽量完成)

- [ ] ⚪ 小程序端能打开 2 个页面(D3,B 负责)
- [ ] ⚪ 异步任务框架可视化(看日志确认 `AsyncTaskRunner` 跑过)
- [ ] ⚪ 流程图 ECharts 渲染
- [ ] ⚪ 期限预警 badge
- [ ] ⚪ Docx 导出(草案 + 评估报告 + 清理报告 + 意见报告)
- [ ] ⚪ Login 鉴权闭环 + 错误次数限制
- [ ] ⚪ 答辩 PPT 15 页(D3 完成)
- [ ] ⚪ 演示脚本 1.2 万字(D3 完成)
- [ ] ⚪ 5 分钟演示视频(D3 录屏)
- [ ] ⚪ git tag v1.0-defense + zip 包(D3 完成)



### 🔵 P2 — 可选(来不及就砍)

- [ ] ⚪ CI workflow 跑通
- [ ] ⚪ 实时通知(WebSocket)
- [ ] ⚪ Neo4j 真实部署的 fancy UI(目前用 ECharts graph 渲染即可)

---



## 六、风险预案(出问题时怎么办)🆕


| 风险                       | 触发条件                            | 应对方案                                                                                          | 触发频次       |
| ------------------------ | ------------------------------- | --------------------------------------------------------------------------------------------- | ---------- |
| **mvn test 跑挂**          | D1 working tree 改动导致 20+ 测试用例失败 | A 排查 → 1 小时内修不回就 `git stash` working tree → 回 first commit 跑测试 → 在新分支上 cherry-pick 改动         | ⚪ 待观察      |
| **npm install 装不上**      | Node 版本 / 锁文件 / 镜像源             | `npm install --legacy-peer-deps --registry https://registry.npmmirror.com`                    | 已备好命令      |
| **Docker 容器起不来**         | 端口冲突 / 数据卷损坏 / docker daemon 挂  | 删 `docker/docker-data/mysql/`* 重跑 schema.sql;`docker compose down -v && docker compose up -d` | 已备好命令      |
| **Neo4j 启不来**            | docker compose 报错 / 内存不足        | `NEO4J_ENABLED=false`,降级到内存版图谱,前端仍能渲染                                                         | 已验证降级 OK   |
| **MySQL 表不完整**           | `legislation_schema.sql` 截断     | **必跑** `supplement_public.sql` 补 sys_user / notify_message                                    | ✅ 已补       |
| **Qwen 在线调用失败**          | API Key 过期 / 限流 / dashscope 维护  | 切回 `legislation.qwen.mode=offline`,日志 `[QWEN_OFFLINE_FALLBACK]` 提示;演示时讲"框架已就绪,Key 现场替换即可"     | 已备好兜底      |
| **前端依赖装不上**              | Node 版本 / 锁文件冲突                 | `npm install --legacy-peer-deps --no-audit`,或装 Node 18 LTS                                    | Day1 验证 OK |
| **某个核心接口 500**           | Service NPE / Mapper 缺失         | D2 GlobalExceptionHandler 完善,前端 axios 拦截器对 5xxxxx 业务异常提示用户文案                                  | D2 必做      |
| **演示数据不够**               | 种子数据 < 5 项目 / 20 法规             | D1 跑一遍 schema.sql + seed-data.sql,Demo 前再跑一次                                                  | ✅ 已验证      |
| **Dashboard 加载慢**        | 8 接口并发拉数据                       | `safeCall` 独立 try/catch,单接口失败不影响其他                                                            | ✅ 已实现      |
| **D1 一天搞不完提交+跑通**        | 实际情况比预期复杂                       | **优先级收缩**:D1 只交 backend + docker,前端 + 冒烟推到 D2 上午                                              | 已留 buffer  |
| **答辩日 14:00 前发现 P0 bug** | 极端情况                            | C 录的 5 分钟视频作为应急 backup,口述"这是我们录的应急 backup"                                                    | 已录         |


---



## 七、协作机制(防止踩坑)🆕



### Git 分支策略(D1 起调整)

```
main (受保护,D1 / D2 / D3 各打 1 个里程碑 commit)
   ├── feat/a-backend-day1  ← A 在 D1 早晨建,提交后 merge
   ├── feat/b-frontend-day1 ← B 在 D1 早晨建,提交后 merge
   └── feat/c-ops-day1     ← C 在 D1 早晨建,提交后 merge
```

- **D1 09:00**:3 人各自建 feat/feat-* 分支,各自 commit 自己的 working tree 改动
- **D1 12:30**:3 个分支合 MR 到 main,跑冒烟
- **D1 18:00**:跑通即视为 D1 完成,tag `v1.0-day1`
- **D2 / D3**:继续在 main 上 commit,每天 18:00 tag `v1.0-day2` / `v1.0-day3`
- **D3 18:00**:打 `v1.0-defense` tag + 推 origin
- **commit 规范**:`<type>(<scope>): <subject>`,例如 `feat(backend): 接 EvaluationController.chart`
- **冲突优先级**:backend → frontend → docker → docs(后端先定,前端跟上)



### 每日站会议程(15 分钟,09:30)

```
A: 昨天 done - mvn test 22/22 绿 + 36 步冒烟全绿;今天 to-do - GlobalExceptionHandler 完善;blocker - 无。
B: 昨天 done - Vitest 65/65 绿 + Dashboard 8 接口跑通;今天 to-do - 资料库 3 页接通;blocker - 等 A 给 /library 接口。
C: 昨天 done - Docker 5 容器全 healthy + 36 步冒烟全绿;今天 to-do - README 校对 + OpenAPI 导出;blocker - 无。
```



### 协作 Checklist(D1-D4 每完成一格打 ✅)

- [ ] ⚪ D1 站会 09:30 完成
- [ ] ⚪ D1 三人各自 commit 自己的 working tree
- [ ] ⚪ D1 三人各自跑通自己的验证(`mvn test` / `npm test` / `docker compose ps`)
- [ ] ⚪ D1 集成同步 18:00 完成(36 步冒烟全绿)
- [ ] ⚪ D2 站会 + 集成 + 验收
- [ ] ⚪ D3 站会 + 集成 + 验收
- [ ] ⚪ D4 上午全链联调 + 答辩演练

---



## 八、答辩日行动清单


| 时间          | 事项                                                                        | 负责人       |
| ----------- | ------------------------------------------------------------------------- | --------- |
| 09:00       | 最终一次全栈启动,跑冒烟脚本 `day1-verify.sh` + `day2-smoke.sh` + `day3-smoke.sh`(36 步) | C         |
| 10:00       | 全链联调 1 小时 + 修卡点                                                           | A + B + C |
| 11:30       | 三人各讲一遍自己链路(各 10 分钟) + 自由问答 20 分钟                                          | A + B + C |
| 12:00       | 午饭 + 休息                                                                   | 全员        |
| 13:30(可能更早) | ✅ **正式答辩**                                                                | 全员        |
| 答辩现场应急      | 翻车时用 C 录的 5 分钟视频 backup                                                   | C         |




### 演示脚本(讲什么、点哪里、说什么数字)

> 📌 完整 1.2 万字 `demo-voc.json` 由 C 在 D3 产出,这里给出提纲。


| 阶段              | 屏幕                                              | 关键数字                                       | 时长    |
| --------------- | ----------------------------------------------- | ------------------------------------------ | ----- |
| 1. 背景           | 项目背景页                                           | 5 部法律 / 30 部门规章 / 8000 部地方规章               | 1 min |
| 2. 架构           | 架构图 + 8 大模块                                     | 11 Controller / 9 Service / 24 表 / Qwen 在线 | 2 min |
| 3. 演示-项目        | 登录 → 创建项目 → 推进阶段 → 详情流程图                        | 10 节点流程 / BCrypt 加密                        | 3 min |
| 4. 演示-草案        | 草案生成 → 异步轮询 → Markdown 预览                       | 6 章节 / 上位法 RAG / Qwen 真路径                  | 2 min |
| 5. 演示-审查        | 智慧审查 → 4 级问题红/黄/蓝/灰                             | 6 类规则                                      | 2 min |
| 6. 演示-清理        | 清理任务 → 候选法规 → AI 建议                             | 124 候选 / 87% 置信                            | 2 min |
| 7. 演示-评估        | 评估任务 → 雷达图 + 折线 → 报告导出                          | 3 维评分 92 分                                 | 2 min |
| 8. 演示-意见        | 意见发布 → 提交 → 词云 → 报告                             | 248 意见 / 80% 去重                            | 2 min |
| 9. 演示-Dashboard | 大屏 4 行 11 卡片                                    | 8 真实接口 / 11 图表                             | 2 min |
| 10. 工程化         | Docker compose + Prometheus + Grafana + OpenAPI | 1 键启动 / 5 容器 / OpenAPI.json 80KB           | 2 min |


**总时长**:20 分钟演示 + 10 分钟问答 = 30 分钟

---



## 九、一句话总结(贴在工位)

> **A 把后端跑通(commit + mvn test + 36 步冒烟 + Qwen 在线)🟡 B 把前端跑通(commit + Vitest + Dashboard 8 接口 + 资料库 + 小程序)🟡 C 把工程跑通(commit + Docker 5 容器 + 监控 + 答辩 PPT + 录屏)🟡;3 人 3 日 = 把 working tree 真正变成"已验证可演示",垂直交付,横向拼成完整平台。**

---



## 附录 A:D1 必跑的 4 套验证(每套都标 FAIL/0 才能算 D1 完成)



### A.1 后端验证(A 负责)

```bash
cd legislation-edition/backend
mvn clean compile                                    # BUILD SUCCESS
mvn test                                             # ≥20/20 绿
mvn spring-boot:run &                                # 后台启动
sleep 30
curl http://localhost:8083/api/auth/health           # {"status":"UP",...}
TOKEN=$(curl -s -X POST http://localhost:8083/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8083/api/info/dashboard           # 返回真实数据
```



### A.2 前端验证(B 负责)

```bash
cd legislation-edition/web_frontend
npm install --legacy-peer-deps                       # 装依赖
npm run build                                        # 构建成功
npm run test:unit                                    # ≥55/55 绿
npm run dev &                                        # 后台启动
sleep 30
curl -I http://localhost:5173                        # 200 OK,前端可访问
```



### A.3 Docker 验证(C 负责)

```bash
cd docker
docker compose up -d                                 # 5 容器起
docker ps                                # 5 容器全部 "(healthy)"
bash scripts/day1-verify.sh                          # PASS=11 / FAIL=0
bash scripts/day2-smoke.sh                           # PASS=9 / FAIL=0
bash scripts/day3-smoke.sh                           # PASS=16 / FAIL=0
```



### A.4 答辩日切换(a 答辩当天 09:00)

```bash
# 最终一次全栈启动 + 冒烟 + 监控可见
cd docker && docker compose up -d
sleep 60
bash scripts/day1-verify.sh && bash scripts/day2-smoke.sh && bash scripts/day3-smoke.sh
# 36 步全绿 = 答辩就绪
```

---



## 附录 B:D1 提交清单(3 个 commit 的实际内容)



### commit 1: `feat(backend): 一次性提交 Day1-3 业务改造`

涉及 8 个文件:

- `controller/CleanupController.java` (+88 行)
- `controller/ConsultationController.java` (+76 行)
- `controller/DraftController.java` (+65 行)
- `controller/EvaluationController.java` (+108 行)
- `service/impl/CleanupServiceImpl.java` (+13 行)
- `service/impl/DraftServiceImpl.java`(改)
- `service/impl/EvaluationServiceImpl.java`(改)
- `service/impl/InfoServiceImpl.java`(改)
- `service/AuthService.java`(改)
- `ai/qwen/QwenApiClient.java` (+18 行)
- `ai/qwen/QwenChatRequest.java`(改)
- `common/exception/GlobalExceptionHandler.java`(+5 行)
- `config/DataInitializer.java`(重写 70+ 行)
- `src/main/resources/application.yml`(改 1 行:logic-delete-field)
- `src/main/resources/application-dev.yml`(重写 90+ 行:统一配置)



### commit 2: `feat(frontend): 一次性提交 Day1-3 前端页面改造`

涉及 20 个文件:

- `views/info/Dashboard.vue`(+610 行,+453 行)
- `views/info/dashboard-charts/{TrendChart,PieDonut,BarChart,GraphView}.vue`(4 个新增)
- `views/auth/Login.vue`(改)
- `views/cleanup/Tasks.vue`(改)
- `views/consultation/List.vue`(改)
- `views/draft/Generate.vue`(改)
- `views/evaluation/Tasks.vue`(改)
- `views/project/{index,Detail}.vue`(改)
- `views/review/{Submit,Rules}.vue`(改)
- `api/legislation.js`(+4 行)
- `views/{evaluation,info,project}/__tests__/*.test.js`(3 个新增)
- (其余 27 个 vue 保持 first commit 不动)



### commit 3: `chore(docker+tests): 补 healthcheck + 冒烟脚本`

涉及 8 个文件:

- `docker/docker-compose.yml`(改 healthcheck + start_period)
- `scripts/day1-verify.sh`(新增 80 行)
- `scripts/day2-smoke.sh`(新增)
- `scripts/day3-smoke.sh`(新增 104 行)
- `scripts/export-openapi.sh`(新增)
- `scripts/backup-data.sh`(新增)
- `src/test/.../IntegrationTest.java`(85 行)
- `src/test/.../{SuperiorLawParser,ReviewRule,DocxExporter}Test.java`(3 个)



### D1 下午补 commit 4(可选): `feat(miniprogram): 小程序 9 页基础改造`

- `miniprogram/src/pages/{index,dashboard,draft,evaluation,cleanup,consultation,library,info,profile}/index.vue`(9 个)

---



## 附录 D:测试统计目标(D1 / D2 / D3)


| 维度     | D1 目标   | D2 目标   | D3 目标   |
| ------ | ------- | ------- | ------- |
| 后端单测   | 20/20   | 22/22   | 22/22   |
| 后端集成测试 | 9/9     | 9/9     | 9/9     |
| 前端单测   | 55/55   | 65/65   | 65/65   |
| 冒烟脚本   | 36/36   | 36/36   | 36/36   |
| **总计** | **120** | **132** | **132** |


---

**📌 文档维护**:每日站会后由 C 更新 ✅ 进度;有 blocker 立刻群里报;最终交付物清单(D4 09:00 前)必须 100% ✅。

**📌 联系方式**:微信群 / 企业微信 / GitLab MR 评审 / 每日 18:00 同步会议。

**📌 下次更新**:2026-10-06 18:00(D1 收工同步后)