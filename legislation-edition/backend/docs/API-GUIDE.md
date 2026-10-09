# Backend · API 分组与运行手册

> 对应 `application.yml` 中 `springdoc.group-configs` 配置。本文件是 **答辩 / 二次开发** 的速查手册。

---

## 1. 启动后端

### 1.1 本地开发

```bash
cd legislation-edition/backend
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev
# 监听 http://localhost:8083/api
```

### 1.2 Docker Compose（推荐）

```bash
cd docker
docker compose up -d backend --force-recreate
# 5 个容器: mysql / neo4j / backend / prometheus / grafana
# backend 健康检查通过后,访问 http://localhost:8083/api
```

### 1.3 健康检查

```bash
curl http://localhost:8083/api/actuator/health
# {"status":"UP"}
```

---

## 2. 鉴权

| 步骤 | 命令 |
|------|------|
| 1. 登录拿 token | `POST /api/auth/login` body=`{username,password}` |
| 2. 后续请求 | Header `Authorization: Bearer <token>` |
| 3. 刷新 | `POST /api/auth/refresh` |

**默认账号**（DataInitializer 自动回填 BCrypt）：
- `admin / 123456` (ROLE_ADMIN)
- `drafter / 123456` (ROLE_USER)
- `reviewer / 123456` (ROLE_REVIEWER)
- `evaluator / 123456` (ROLE_EVALUATOR)
- `leader / 123456` (ROLE_LEADER)
- `user1 / 123456` (普通用户)

---

## 3. API 分组（9 大模块）

| # | 模块 | URL 前缀 | 业务能力 |
|---|------|----------|----------|
| 1 | 立法项目 | `/legislative-project` | 项目立项 / 流程推进 / 阶段模板 / 仪表盘 |
| 2 | 草案生成 | `/draft` | AI 草案生成（异步 taskId）/ 版本管理 |
| 3 | 智慧审查 | `/review` | 规则审查 / 问题记录 / 复审 |
| 4 | 智能清理 | `/cleanup` `/regulation` | 清理任务 / 法规上下位 / 引用关系 |
| 5 | 实施评估 | `/evaluation` | 评估任务 / 指标 / 图表 |
| 6 | 意见征集 | `/consultation` | 征集发布 / 意见提交 / 去重 / 词云 / 报告 |
| 7 | 资料库 | `/library` | 法规 / 报告 / 专家意见 / 典型案例 |
| 8 | 信息展示 | `/info` | 立法动态 / 政策解读 / 仪表盘 / 地图 |
| 公共 | 认证 | `/auth` | 登录 / 刷新 / 健康 |

---

## 4. OpenAPI 文档

### 4.1 Knife4j UI（推荐）

容器启动后访问：**http://localhost:8083/api/doc.html**

特性：
- 中文界面（`zh_cn`）
- 9 个分组（按业务模块）
- 支持离线导出（Markdown / HTML / OpenAPI JSON）
- 持久化 Authorization（`persist-authorization: true`，登录后 token 跨刷新保留）

### 4.2 OpenAPI 3 原始 JSON

```
GET /api/v3/api-docs           # 默认分组
GET /api/v3/api-docs?group=01-立法项目
```

可配合前端代码生成工具（`openapi-generator`）。

### 4.3 Swagger UI（备用）

```
http://localhost:8083/api/swagger-ui.html
```

---

## 5. 关键接口速查

| 能力 | Method | Path |
|------|--------|------|
| 用户登录 | POST | `/auth/login` |
| 项目立项 | POST | `/legislative-project` |
| 项目分页 | GET | `/legislative-project/list` |
| 项目仪表盘 | GET | `/legislative-project/dashboard` |
| 推进到下一阶段 | POST | `/legislative-project/{id}/advance` |
| 即将到期项目 | GET | `/legislative-project/upcoming` |
| 草案生成（异步） | POST | `/draft/generate` |
| 草案任务状态 | GET | `/draft/task/{taskId}` |
| 审查规则列表 | GET | `/review/rule/list` |
| 提交审查 | POST | `/review/submit` |
| 清理任务列表 | GET | `/cleanup/task/list` |
| 法规检索 | GET | `/regulation/search` |
| 评估任务 | POST | `/evaluation/task` |
| 评估图表 | GET | `/evaluation/chart` |
| 意见征集 | GET | `/consultation/list` |
| 提交意见 | POST | `/consultation/{id}/opinion` |
| 词云 | GET | `/consultation/{id}/wordcloud` |
| 资料库 | GET | `/library/material/list` |
| 立法动态 | GET | `/info/news` |
| 仪表盘 | GET | `/info/dashboard` |
| 地图分布 | GET | `/info/map/regulation` |

---

## 6. Actuator 与监控

| 端点 | 权限 | 用途 |
|------|------|------|
| `/actuator/health` | 公开 | K8s 探活 |
| `/actuator/info` | 公开 | 应用元信息 |
| `/actuator/prometheus` | 公开 | Prometheus scrape |
| `/actuator/threaddump` | **ADMIN** | 线程栈 |
| `/actuator/heapdump` | **ADMIN** | 堆快照 |
| `/actuator/metrics/**` | **ADMIN** | Micrometer 指标浏览 |

**生产建议**：
- 只保留 `health/info/prometheus` 对外
- threaddump/heapdump 仅内网或 ADMIN 访问
- Prometheus + Grafana 看板见 `docker/grafana/`

---

## 7. 缓存配置

`application.yml`：
```yaml
legislation:
  cache:
    type: memory   # memory (默认) | redis
```

切换 Redis：
1. 加 `spring-boot-starter-data-redis` 依赖
2. 配置 `spring.data.redis.host/port/password`
3. `CacheConfig` 中换 `RedisCacheManager` 实现
4. 当前用 `ConcurrentMapCacheManager`（JVM 内存），重启即失效

当前缓存键：
- `project:dashboard` 项目仪表盘聚合
- `regulation:index`  法规索引
- `info:dashboard`    信息门户 dashboard
- `evaluation:chart`  评估图表

---

## 8. 异常与日志约定

- **业务异常**：`throw new BizException(40400, "项目不存在")` → 自动翻译为 `Result(code=40400, message="项目不存在")`
- **校验失败**：`@Valid`/`@Validated` 失败 → 40001 + 字段错误明细
- **未捕获异常**：仅记日志类名+消息，不打印堆栈（防止泄露 SQL/路径）
- **SQL 日志**：`mybatis-plus.configuration.log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl`（写日志文件，不打 stdout）

---

## 9. 单元测试

```bash
cd legislation-edition/backend
mvn test
# 当前: Tests run: 37, Failures: 0
```

测试覆盖：
- `IntegrationTest` (11) 端到端（openapi profile，H2）
- `ConsultationServiceWordCloudTest` (7) 词云切词 Mockito
- `ResultAndBizExceptionTest` (8) 通用响应
- `ReviewRuleTest` (5) 审查规则
- `SuperiorLawParserTest` (4) 上位法解析
- `DocxExporterTest` (2) Word 导出

---

## 10. 相关文件

- `application.yml` — 主配置（含 springdoc 分组、cache、actuator）
- `pom.xml` — Maven 依赖
- `src/main/java/com/legal/legislation/common/exception/` — 异常定义
- `src/main/java/com/legal/legislation/config/SecurityConfig.java` — 安全
- `src/main/java/com/legal/legislation/config/CacheConfig.java` — 缓存
