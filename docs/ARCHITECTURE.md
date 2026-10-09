# 智立法 · 平台架构

> 本文件面向答辩 / 二次开发同学，使用 C4 模型思路描述系统。

---

## 1. Context（系统上下文）

```
┌──────────────────────────────────────────────────────────┐
│                       智立法 平台                         │
│                                                          │
│  立法机关人员        普通公众        法规爬虫              │
│      │                │                │                 │
│      ▼                ▼                ▼                 │
│  ┌──────┐         ┌──────┐         ┌────────┐           │
│  │ Web  │         │ 小程序│         │ Crawler│           │
│  └──────┘         └──────┘         └────────┘           │
│      │                │                │                 │
│      └────────────────┼────────────────┘                 │
│                       ▼                                  │
│               Backend (Spring Boot)                     │
│                       │                                  │
│                       ▼                                  │
│              MySQL + Neo4j + Redis(可选)                 │
└──────────────────────────────────────────────────────────┘
```

**角色**：
- **立法机关人员**：通过 Web 端管理项目、审查、评估、清理
- **普通公众**：通过小程序提交意见、查看公开数据
- **法规爬虫**：定时抓取 gov.cn / moj.gov.cn 等法规源

---

## 2. Container（容器视图）

```
┌──────────────────────────────────────────────────────────────────┐
│  Docker Compose (legislation-platform/docker)                     │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │  MySQL 8.0   │    │ Neo4j 5.15   │    │  Spring Boot 3   │   │
│  │  3306        │◀──▶│  7687/7474   │    │  8083 (/api)     │   │
│  │  24 张业务表  │    │  法规知识图谱 │◀──▶│  Java 17 JRE     │   │
│  └──────────────┘    └──────────────┘    └────────┬─────────┘   │
│                                                     │             │
│  ┌──────────────────┐    ┌──────────────────────┐  │             │
│  │  Prometheus      │◀───│  /actuator/prometheus│◀─┘             │
│  │  9090            │    └──────────────────────┘                │
│  └──────────────────┘                                            │
│           │                                                      │
│  ┌────────▼─────┐                                                │
│  │  Grafana     │                                                │
│  │  3000        │                                                │
│  └──────────────┘                                                │
└──────────────────────────────────────────────────────────────────┘
```

**核心容器**：
- **MySQL 8.0**：24 张业务表（`legal_legislation` schema）
- **Neo4j 5.15**：法规上下位 / 引用 / 替代关系图
- **Spring Boot 3**：Java 17，10 个 Controller，~95 个 API
- **Prometheus + Grafana**：指标采集与看板

---

## 3. Component（后端组件）

```
backend/src/main/java/com/legal/legislation/
├── LegislationApplication.java       # 启动类
├── common/
│   ├── Result.java                   # 统一响应 {code, message, data}
│   ├── PageReq.java                  # 分页请求
│   ├── PageResp.java
│   └── exception/
│       ├── BizException.java         # 业务异常 (4xxxxx/5xxxxx)
│       └── GlobalExceptionHandler.java  # @RestControllerAdvice
├── config/
│   ├── SecurityConfig.java           # JWT + @PreAuthorize
│   ├── MybatisPlusConfig.java        # 分页插件
│   ├── CacheConfig.java              # ConcurrentMapCacheManager
│   ├── Neo4jConfig.java              # Neo4j Driver
│   ├── WebMvcConfig.java             # CORS
│   ├── OpenApiConfig.java
│   └── DataInitializer.java          # seed admin 账号
├── controller/   (10 个)             # REST API
│   ├── LegislativeProjectController  # 立法项目
│   ├── DraftController               # 草案生成
│   ├── ReviewController              # 智慧审查
│   ├── CleanupController             # 智能清理
│   ├── EvaluationController          # 实施评估
│   ├── ConsultationController        # 意见征集
│   ├── LibraryController             # 资料库
│   ├── InfoController                # 信息展示
│   ├── AuthController                # 认证
│   └── CrawlerAdminController        # 爬虫管理 (ADMIN)
├── service/ + impl/  (10 个)         # 业务逻辑
├── mapper/  (24 个)                  # MyBatis-Plus
├── entity/  (22 个)                  # @TableName 实体
├── graph/                             # Neo4j 子图服务
│   └── RegulationGraphService
├── notify/                            # 站内消息
│   ├── NotifyMessage
│   ├── NotifyService
│   └── ConsoleNotificationServiceImpl
├── metrics/                           # Micrometer 业务指标
│   └── BusinessMetrics
├── audit/                             # 操作审计 (AOP)
│   ├── Audited
│   ├── AuditLog
│   └── AuditAspect
├── security/                          # JWT
│   ├── JwtUtil
│   ├── JwtAuthFilter
│   └── ...
├── task/                              # 定时任务
│   └── DeadlineReminderTask
├── util/                              # 工具类
└── ai/qwen/                           # 通义千问 (Phase 2)
    ├── QwenApiClient
    ├── OfflineQwenClient
    └── ...
```

---

## 4. 数据流：意见征集 → 词云（典型案例）

```
[公众]
  │  POST /api/consultation/{id}/opinion  body={content, name, contact}
  ▼
[ConsultationController]
  │  consultationService.submitOpinion(...)
  ▼
[ConsultationServiceImpl]
  │  1) simHash(content)                  ← 去重
  │  2) opinionMapper.insert(...)         ← 持久化
  │  3) classifyByQwen(content)           ← AI 归类 (离线规则)
  ▼
[opinion 表]   ← MySQL

--- 词云 ---
[Admin]
  │  GET /api/consultation/1/wordcloud
  ▼
[ConsultationController]
  │  consultationService.wordCloud(1, 50)
  ▼
[ConsultationServiceImpl.wordCloud]
  │  1) selectList(QueryWrapper.eq("consultation_id", 1))
  │  2) 清洗标点: replaceAll("[\\p{P}\\p{S}\\s]+", "")
  │  3) 滑窗 2~4 字短语
  │  4) 过滤 50 停用词
  │  5) 过滤含数字
  │  6) freq 排序 → topN
  ▼
[{name: "数据安全", value: 12}, ...]
  │
  ▼
[ECharts WordCloud 组件]
```

---

## 5. 部署

### 5.1 单机 Docker（开发 / 答辩）

```bash
cd docker
docker compose up -d          # 5 容器全起
```

资源占用（实测）：
- MySQL:  ~300MB
- Neo4j:   ~500MB
- Backend: ~512MB  (JAVA_OPTS: -Xms256m -Xmx768m)
- Prometheus/Grafana: ~250MB

### 5.2 生产建议

| 组件 | 建议 |
|------|------|
| Backend | 水平扩展（无状态），前置 Nginx |
| MySQL | 主从 + 慢查询监控 |
| Neo4j | 集群（≥3 节点）Causal Clustering |
| Redis | 启用（替换 `ConcurrentMapCacheManager`） |
| 监控 | Prometheus + Alertmanager（backend 不健康告警） |
| 日志 | ELK / Loki，SQL 日志走 SLF4J 文件 |

---

## 6. 安全设计

- **认证**：JWT (HS256) + `JwtAuthFilter`，Token 24h 过期
- **授权**：`@EnableMethodSecurity(prePostEnabled=true)` + `@PreAuthorize("hasRole('ADMIN')")`
- **密钥**：`jwt.secret` 32 字节，生产通过 `JWT_SECRET` 环境变量覆盖
- **Actuator**：`threaddump/heapdump` 限 ADMIN
- **CORS**：`legislation.cors.allowed-origins: "*"`（生产应白名单）
- **SQL 注入**：MyBatis-Plus `#{}` 参数化（无 `${}` 拼接）
- **密码**：BCrypt
- **异常**：不打印堆栈、不回显 SQL，避免信息泄露

---

## 7. 关键技术决策

| 决策 | 理由 |
|------|------|
| MyBatis-Plus 而非 JPA | 业务方有大量自定义 SQL（监管类查询），MP 动态条件更顺手 |
| 单独 Neo4j 存图谱 | 法规关系是天然的图结构；用 SQL 模拟要走多次 JOIN |
| 单服务而非微服务 | 9 个业务模块耦合高，团队规模小，避免分布式复杂度 |
| 无 LLM 也能跑 | `OfflineQwenClient` 用规则引擎做 AI 占位，QWEN_API_KEY 缺失时降级 |
| Spring Cache + JVM 内存 | 答辩场景 5 容器，Redis 增重不增价值；留切换口 |
| Knife4j + springdoc | 国内团队主流，导出离线文档，OpenAPI 3 兼容 |
| 9 个 Controller 1 个 ServiceImpl = 1 Controller | 后期按业务域拆包，符合 Single Responsibility |
