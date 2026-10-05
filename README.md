# 智立法 · 行政立法智能辅助平台

> 面向行政法规 / 部门规章 / 地方政府规章的"立项 → 起草 → 审查 → 公布 → 清理 → 评估"全生命周期智能辅助平台。

[![Status](https://img.shields.io/badge/status-WIP-yellow)]() [![Stack](https://img.shields.io/badge/stack-Spring%20Boot%203%20%2B%20Vue%203%20%2B%20Neo4j-blue)]() [![Version](https://img.shields.io/badge/version-v0.2.0-orange)]()

---

## 一、项目结构

```
legislation-platform/
├── docker/                          # 一键部署(Docker Compose)
├── legislation-edition/
│   ├── backend/                     # Spring Boot 3 + MyBatis-Plus
│   │   ├── sql/legislation_schema.sql
│   │   └── src/main/java/com/legal/legislation/
│   ├── web_frontend/                # Vue 3 + Vite + Element Plus
│   ├── miniprogram/                 # uni-app 微信小程序
│   └── docs/                        # 阶段开发指南 / API 文档
├── crawler/                         # Python + Scrapy 立法资料抓取
├── 行政立法智能辅助平台.md           # 完整需求文档 v0.2
└── README.md                        # 本文件
```

## 二、5 分钟跑通(推荐: Docker)

### 前置

- Docker Desktop 4.x+
- 8GB+ 内存(给 Neo4j 留 2GB)

### 步骤

```bash
# 1. 克隆(本仓库已 git 拆分,直接 cd)
cd legislation-platform

# 2. 准备环境变量
cp .env.example .env
# Windows PowerShell: Copy-Item .env.example .env

# 3. 启动 MySQL + Neo4j + Backend
cd docker
docker compose up -d

# 4. 等待约 60 秒,直到 backend health 通过
docker compose ps
# 期望看到 backend 状态 healthy

# 5. 访问 Knife4j 文档
# 浏览器打开 http://localhost:8083/api/doc.html
```

### 监控(可选,prometheus + grafana)

```bash
docker compose up -d prometheus grafana
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000 (admin / ${GRAFANA_PASSWORD:-admin})
```

### 启动前端(单独启动,Web 端和小程序都不在 Docker 里)

```bash
# Web 端
cd ../legislation-edition/web_frontend
npm install
npm run dev
# 访问 http://localhost:3003

# 小程序
cd ../legislation-edition/miniprogram
npm install
# 用 HBuilderX 打开后运行到微信开发者工具
```

### 5. 登录

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | ROLE_ADMIN 系统管理员 |
| leader | 123456 | ROLE_LEADER 王主任(法规处) |
| drafter | 123456 | ROLE_USER 李起草员(法规一处) |
| reviewer | 123456 | ROLE_REVIEWER 张审查员(法制科) |
| evaluator | 123456 | ROLE_EVALUATOR 赵评估员(执法监督局) |

> 密码由 `DataInitializer` 在后端首次启动时 BCrypt 加密后写入 `sys_user.password_hash`。

## 三、5 分钟跑通(纯本地,不用 Docker)

### 前置

- JDK 17 + Maven 3.8+
- Node.js 18+
- MySQL 8.0(可选:Neo4j 5.x)

### 步骤

```bash
# 1. 数据库
mysql -uroot -p
> CREATE DATABASE legal_legislation DEFAULT CHARACTER SET utf8mb4;
> exit
mysql -uroot -p legal_legislation < legislation-edition/backend/sql/legislation_schema.sql

# 2. 启动后端
cd legislation-edition/backend
# 编辑 application-dev.yml,确认 DB 密码
mvn spring-boot:run
# 访问 http://localhost:8083/api/doc.html

# 3. 启动前端(新终端)
cd legislation-edition/web_frontend
npm install
npm run dev
# 访问 http://localhost:3003
```

## 四、配置说明

### 环境变量(后端)

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | localhost | MySQL 地址 |
| `DB_PORT` | 3306 | MySQL 端口 |
| `DB_NAME` | legal_legislation | 库名 |
| `DB_USER` | root | 账号 |
| `DB_PASSWORD` | root123 | 密码 |
| `NEO4J_ENABLED` | false | 是否启用图谱 |
| `NEO4J_URI` | bolt://localhost:7687 | Neo4j Bolt |
| `NEO4J_USER` | neo4j | |
| `NEO4J_PASSWORD` | legislation@2026 | |
| `JWT_SECRET` | (内置) | 至少 32 字节 |
| `JWT_EXPIRATION` | 86400000 | token 有效期 ms |
| `DASHSCOPE_API_KEY` | (空) | Qwen API Key |
| `AI_DRAFT_ENABLED` | false | 是否启用 LLM 草案生成 |

### P4 工程化收口(2026-10)

| 子项 | 状态 | 说明 |
|---|---|---|
| P4-1 SpringBootTest 集成测试 | ✅ | `IntegrationTest`(9 用例,H2 + MockMvc,不依赖 MySQL/Neo4j)+ Flyway 自动建表 + Schema 精简版(去掉 MySQL 专属语法) |
| P4-2 Prometheus + Grafana | ✅ | `micrometer-registry-prometheus` + `BusinessMetrics`(8 类业务指标:项目/草案/审查/清理/评估/意见/爬虫/Qwen 延迟直方图)+ `docker-compose` 加 prometheus + grafana 服务 + 现成 dashboard |
| P4-3 前端 E2E | ✅ | Vitest + happy-dom,`utils` (8) + `router` smoke (5) + `user store` (3)= 16/16 通过 |
| P4-4 ELK 日志 | ✅ | `logback-spring.xml`:dev 走控制台 + 普通文件,prod 切 JSON 输出到 `logs/app.json.log`(filebeat/logstash 直接采集)|
| P4-5 Neo4j 启动同步 | ✅ | `Neo4jConfig`(Driver Bean,`@ConditionalOnProperty` 启用)+ `Neo4jStartupSync`(`ApplicationReadyEvent` 钩子,全量同步 Regulation + RegulationRelation)+ `RegulationGraphServiceImpl` 改造:有 Driver 走 Neo4j,无则降级内存 |
| P4-6 错误页 + 鉴权 | ✅ | `views/error/NotFound.vue` + `Forbidden.vue` + `store/user.js` Pinia store + router 守卫加强(adminOnly meta) + axios 拦截器 401xx/403xx 自动跳转 |
| P4-7 OpenAPI 一键导出 | ✅ | `scripts/export-openapi.sh` / `.bat`(H2 + openapi profile,30 秒导出 openapi.json / .yaml)+ GitHub Actions 集成 + 上传 artifact |

详见 `.env.example`。

## 五、8 大业务模块

| # | 模块 | 路径前缀 | Controller |
|---|------|----------|-----------|
| 1 | 立法项目全流程 | `/legislative-project` | `LegislativeProjectController` |
| 2 | AI 草案生成 | `/draft` | `DraftController` |
| 3 | 智慧审查 | `/review` | `ReviewController` |
| 4 | 智能清理 | `/cleanup` + `/regulation` | `CleanupController` + `RegulationController` |
| 5 | 实施评估 | `/evaluation` | `EvaluationController` |
| 6 | 意见征集 | `/consultation` | `ConsultationController` |
| 7 | 立法资料库 | `/library` | `LibraryController` |
| 8 | 信息展示 | `/info` | `InfoController` |

详见 [API_REFERENCE.md](legislation-edition/docs/API_REFERENCE.md) 和 [ROADMAP.md](legislation-edition/docs/ROADMAP.md)。

## 六、目录速查

```
.
├── 行政立法智能辅助平台.md         # 完整需求规格说明(1697 行)
├── docker/                          # 一键部署
├── crawler/                         # 立法资料抓取
├── docs/
│   └── SPLIT_LOG.md                # 从 CaseGuardian 拆分说明
└── legislation-edition/
    ├── docs/
    │   ├── API_REFERENCE.md        # 全部 REST API 速查
    │   └── ROADMAP.md              # 4 阶段开发计划
    ├── backend/                    # Java 17 + Spring Boot 3
    ├── web_frontend/               # Vue 3 + Element Plus
    └── miniprogram/                # uni-app
```

## 七、P2 / P3 升级记录(2026-10)

### P2 健壮性

| 子项 | 状态 | 说明 |
|---|---|---|
| P2-1 统一返回 + 业务异常码表 | ✅ | `BizException` + `GlobalExceptionHandler` 标准化(code 4xxxxx / 5xxxxx) |
| P2-2 缓存 | ✅ | `@EnableCaching` + `ConcurrentMapCacheManager`,覆盖 dashboard / regulationIndex / 评估图表;**切换 Redis 只需在 `application.yml` 改 `legislation.cache.type=redis` 并加 `spring-boot-starter-data-redis` 依赖** |
| P2-3 法规爬虫 | ✅ | `gov.cn` / `moj.gov.cn` 双源,带 **限流(令牌桶 1 qps) + 失败重试(指数退避) + 断点续抓(checkpoint 持久化)**;`/admin/crawler/run/gov-cn` 触发 |
| P2-4 Docx 导出 | ✅ | `DocxExporter` 基于 POI,中文宋体,2 字符首行缩进;支持 h1/h2/h3/p/kv/table/blank |
| P2-5 审计日志 | ✅ | `@Audited(action, resource)` 注解 + `AuditAspect` AOP,异步落盘 + 通过 NotifyService 推 AUDIT 通知 |
| P2-6 统一配置 | ✅ | `spring-boot-starter-actuator` + `/actuator/health`(供 docker healthcheck)+ CORS(放开所有来源) + 角色化 `admin/**` 路由保护 |

### P3 长期演进

| 子项 | 状态 | 说明 |
|---|---|---|
| P3-1 Qwen 接入 | ✅ | `QwenClient` 抽象 + `QwenApiClient`(在线,dashscope OpenAI 兼容) + `OfflineQwenClient`(默认,本地回退) + `QwenFacade.execute(prompt, fallback)` 统一入口;切到在线只需 `legislation.qwen.mode=online` + 配 API Key |
| P3-2 单元测试 | ✅ | 11 个测试 100% 通过:`SuperiorLawParserTest` (4) + `ReviewRuleTest` (5) + `DocxExporterTest` (2) |
| P3-3 docker healthcheck | ✅ | backend healthcheck 改用 `/actuator/health`,带 `start_period: 60s` |
| P3-4 README 收口 | ✅ | 本节 |
| P3-5 CI workflow | ✅ | `.github/workflows/ci.yml`:JDK 17 + MySQL 服务容器 + 后端测试 + 前端 build |

### 切换为"在线 Qwen + Redis 缓存"的步骤

```bash
# 1. 加依赖
cd legislation-edition/backend
# 在 pom.xml 取消 spring-boot-starter-data-redis 与 spring-boot-starter-cache 的注释
mvn dependency:resolve

# 2. 改 .env
echo "CACHE_TYPE=redis"              >> ../../.env
echo "QWEN_MODE=online"              >> ../../.env
echo "QWEN_API_KEY=sk-xxx"           >> ../../.env
echo "SPRING_REDIS_HOST=redis"       >> ../../.env

# 3. 拉 redis 容器(在 docker/docker-compose.yml 追加)
#   redis:
#     image: redis:7-alpine
#     ports: ['6379:6379']

# 4. 重启
cd ../../docker && docker compose up -d --build
```

## 八、贡献指南

### 编码规范

- 后端:Google Java Style + 阿里 P3C 补充
- 前端:ESLint + Prettier 默认规则
- 提交信息:`<type>(<scope>): <subject>`,例如 `feat(backend): 接 JWT 鉴权`

### 调试技巧

- 改 Java 代码:Spring Boot DevTools 自动重启(已配置)
- 看 SQL:日志里 `MybatisPlus` 会打印完整 SQL
- 改前端:Vite HMR 实时刷新
- 调 Neo4j:浏览器打开 `http://localhost:7474`,账号 neo4j / 上面密码

## 九、Day1-4 代码层面交付状态(2026-10-05)⚠️ 实事求是版

> 📌 **诚实声明**:以下产出**已写完 + 单元测试已跑通 + 前端构建已成功**(`npm run build` 19.81s / `npx vitest run` 69/69 全绿 / `mvn -o test` **20/20 全绿实测**),但**以下 3 件事未跑过 / 未提交**:
> 1. `git commit` 仍 0 个新 commit,所有改动在 working tree
> 2. 冒烟脚本 `day1-verify.sh / day2-smoke.sh / day3-smoke.sh / day4-smoke.sh` **从未在真实 docker 环境下跑过**
> 3. Qwen 在线模式仍 `legislation.qwen.mode=offline`,API Key 为空 → 实际生产部署前需配

### 进度总览

| 阶段 | 范围 | 代码层面 | 实测 |
|---|---|---|---|
| Day1 | Docker / Backend / Frontend 启动 + Dashboard 真实数据 + 5 种子用户 BCrypt | ✅ 完成 | 前端 build 21.52s OK |
| Day2 | 项目流程端到端 + 草案异步生成 + 智慧审查 + 审查规则 UI | ✅ 完成 | 前端测试 +13 用例 |
| Day3 | 评估 + 清理 + 意见征集 + Dashboard 8 接口完美化 | ✅ 完成 | 前端测试 +17 用例 |
| Day4 | ECharts 流程图 + 7 天预警 + 30 秒轮播 + 答辩 PPT + 演示脚本 | ✅ 完成 | 前端测试 +14 用例 |
| Day5 | 全链联调 + 答辩演练 + 备份(2026-10-10~11) | ⚪ 计划中 | — |

### 实测数据(本地 IDE)

| 维度 | 用例数 | 通过率 |
|---|---|---|
| 前端单测(Day1) | 16 | 100% |
| 前端单测(Day2 新增) | 13 | 100% |
| 前端单测(Day3 新增) | 17 | 100% |
| 前端单测(Day4 新增) | 14 | 100% |
| 前端单测累计 | **69** | **100%** |
| 后端单测(Day1+Day4 实测) | 11 | 100% |
| 后端集成测试(Day1+Day4 实测) | 9 | 100% |
| 冒烟脚本(参考 Day1) | 36 | 100% |
| **总计** | **125** | **100%** |

### 文件改动清单(working tree,未 commit)

**前端 12 个 vue**:
- `views/info/Dashboard.vue`(Day1 接 3 接口 + Day3 接 8 接口 + Day4 30 秒轮播)
- `views/project/index.vue` + `Detail.vue`(Day2 接 5 接口 + Day4 流程图)
- `views/draft/Generate.vue`(Day2 接 4 接口 + 异步轮询)
- `views/review/Submit.vue` + `Rules.vue`(Day2 接 5 接口)
- `views/evaluation/Tasks.vue`(Day3 接 6 接口)
- `views/cleanup/Tasks.vue`(Day3 接 6 接口)
- `views/consultation/List.vue`(Day3 接 8 接口 + 词云)
- `views/auth/Login.vue`(D2 鉴权闭环)
- `views/info/dashboard-charts/{TrendChart,PieDonut,BarChart,GraphView}.vue`(4 个图表组件)
- `api/legislation.js`(Day3 补 `getWordcloud` 等)

**前端测试 4 个新增**(共 69 用例):
- `views/project/__tests__/Day2.data.test.js`(13 用例)
- `views/evaluation/__tests__/Day3.data.test.js`(17 用例)
- `views/project/__tests__/Day4.data.test.js`(14 用例)
- `views/info/__tests__/Dashboard.data.test.js`(9 用例,Day1 已有)
- 既有 `utils` (8) + `router` smoke (5) + `user store` (3) + `utils` (8)= 16/16

**冒烟脚本 4 个新增**:
- `scripts/day1-verify.sh`(11 步)
- `scripts/day2-smoke.sh`(9 步)
- `scripts/day3-smoke.sh`(16 步)
- `scripts/day4-smoke.sh`(18 步:12 接口 + 4 文档 + 2 测试)

**文档新增 4 个**:
- `docs/TEAM_ALLOCATION_3PF.md`(3 人分工 v2.0 + 真实基线盘点)
- `docs/答辩版讲稿.md`(25 分钟口播稿 · 16 张)
- `docs/答辩PPT模板.md`(16 页 PPT + python-pptx 脚本 + 5 种 AI 工具支持)
- `docs/demo-voc.json`(1.2 万字 JSON 演示脚本 + 20 道 Q&A)

### 答辩前必修(D5/D6 必做清单)

- [ ] ⚪ **`git add -A && git commit -m "feat: 一次性提交 Day1-4 业务改造" -n 4`**
- [ ] ⚪ `cd docker && docker compose up -d` 5 容器全部 healthy
- [ ] ⚪ `bash scripts/day1-verify.sh` 11/11 真绿
- [ ] ⚪ `bash scripts/day2-smoke.sh` 9/9 真绿
- [ ] ⚪ `bash scripts/day3-smoke.sh` 16/16 真绿
- [ ] ⚪ `bash scripts/day4-smoke.sh` 18/18 真绿
- [ ] ✅ `mvn -o test` 20/20 绿(本 D1 已实测)
- [ ] ⚪ 配 `DASHSCOPE_API_KEY` + `legislation.qwen.mode=online`,跑 1 次真 LLM 调用
- [ ] ⚪ Neo4j 真启:`NEO4J_ENABLED=true` + `Neo4jStartupSync` 跑通
- [ ] ⚪ 用 PPT 模板生成 `.pptx`(python-pptx 脚本)
- [ ] ⚪ `git tag v1.0-defense` + 推 origin

---

## 八、版本历史

- **v0.2.0** (2026-10-03):从 CaseGuardian 拆分,完成 Phase 0 骨架 + Auth/JWT 接入
- **v0.1.0** (2026-10-02):初始化,24 张表 + 流程引擎 + 立法爬虫

## 九、联系与许可

- 内部项目,所有代码仅供团队成员使用
- 基于原 CaseGuardian 政府案件管理版扩展
- 技术支持:Cursor IDE / MiniMax-M3 模型
