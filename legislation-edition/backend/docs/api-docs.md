# API 文档（OpenAPI / Knife4j）

> 智立法 · 行政立法智能辅助平台 —— 后端 API 在线文档使用与团队共享指南

本项目已集成 **springdoc-openapi 2.3.0** + **Knife4j 4.4.0**，启动服务后自动产出 OpenAPI 3 规范文档与可视化 UI，方便前后端、测试、产品同步设计与联调。

---

## 1. 访问入口

启动 `LegislationApplication`（默认端口 `8083`，上下文 `/api`）后，可通过以下地址访问：

| 类型 | 地址 |
|------|------|
| **Knife4j UI（推荐，团队首选）** | <http://localhost:8083/api/doc.html> |
| Swagger UI（原版） | <http://localhost:8083/api/swagger-ui.html> |
| OpenAPI JSON 规范 | <http://localhost:8083/api/v3/api-docs> |
| OpenAPI YAML 规范 | <http://localhost:8083/api/v3/api-docs.yaml> |

> 上下文路径来自 `application.yml` 的 `server.servlet.context-path=/api`，所有文档路径会自动加上该前缀。

---

## 2. 文档分组（Tag）

按业务模块划分（已在 `OpenApiConfig` 中通过 `tags` 元数据定义）：

| Tag | 模块 | 控制器 |
|----|------|--------|
| `00-认证`         | 登录、Token 签发       | `AuthController` |
| `01-立法项目`     | 立项 / 流程推进 / 模板  | `LegislativeProjectController` |
| `02-草案生成`     | LLM 草案生成（异步）   | `DraftController` |
| `03-智慧审查`     | 规则 + AI 审查        | `ReviewController` |
| `04-法规清理`     | 智能清理 / 法规主表    | `CleanupController` / `RegulationController` |
| `05-实施评估`     | 后评估任务 / 报告      | `EvaluationController` |
| `06-意见征集`     | 公开征集 / 分类 / 去重 | `ConsultationController` |
| `07-立法资料库`   | 资料 / 标签 / 收藏     | `LibraryController` |
| `08-信息门户`     | 新闻 / 解读 / 推荐     | `InfoController` |

---

## 3. 团队共享方式

### 方式 A：在线 Knife4j UI（推荐，实时同步）

启动后端后直接把以下地址发给团队成员（注意替换 IP / 域名）：

```
http://<server-ip>:8083/api/doc.html
```

UI 已开启 `persist-authorization`，登录一次后 token 会保存在本地浏览器。

> 部署在内网 / 测试环境时，让运维将 `8083` 端口映射为 HTTPS 域名即可。

### 方式 B：导出 OpenAPI 文件离线分享

在 Knife4j UI 顶部菜单「**文档管理 → 离线文档**」可一键导出：

- **OpenAPI JSON**（推荐用于 Apifox / Postman / Stoplight）
- **Markdown / HTML**（适合直接发邮件或钉钉知识库）

也可以通过命令行直接拉取：

```bash
curl http://localhost:8083/api/v3/api-docs -o openapi.json
```

或浏览器访问 `http://localhost:8083/api/v3/api-docs`，另存为即可。

### 方式 C：导入到 Apifox / Postman

1. 拉取 `openapi.json`（见方式 B）。
2. **Apifox**：`项目设置 → 导入数据 → OpenAPI/Swagger`，选择文件即可生成接口用例。
3. **Postman**：`File → Import → Link / File`，粘贴 URL 或选择文件。
4. **Apipost / Stoplight / Insomnia** 同样支持 OpenAPI 3 导入。

### 方式 D：随仓库提交（用于纯文档分享）

也可以把 `openapi.json` 提交到 `docs/api/openapi.json` 目录，方便在 GitHub / GitLab 直接预览（推荐使用 [Swagger UI](https://github.com/swagger-api/swagger-ui) 在线渲染）。

---

## 4. 给前端同学的约定

### 4.1 统一返回结构

所有接口均返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* 业务数据 */ }
}
```

### 4.2 操作人识别

当前阶段（无 JWT）通过请求头识别操作人：

| Header | 用途 | 示例 |
|--------|------|------|
| `X-User-Id` | 操作人主键 | `1` |
| `Authorization` | 后续接入 JWT 后启用，格式 `Bearer <token>` | `Bearer eyJhbGciOi...` |

### 4.3 异步任务

`/draft/generate`、`/review/submit`、`/consultation/{id}/classify` 等接口为**异步任务**，接口仅返回 `taskId` + `status: PROCESSING`，前端需轮询或订阅 WebSocket（`/ws/...`）获取最终结果。

---

## 5. 配置变更说明

涉及到的关键文件：

| 文件 | 作用 |
|------|------|
| `pom.xml` | 新增 `springdoc-openapi-starter-webmvc-ui` 与 `knife4j-openapi3-jakarta-spring-boot-starter` |
| `config/OpenApiConfig.java` | 元信息（标题、版本、Tag、Server、安全方案） |
| `config/SecurityConfig.java` | 放行 `/v3/api-docs/**`、`/swagger-ui/**`、`/doc.html` |
| `resources/application.yml` | `springdoc.*` + `knife4j.setting.*` 行为配置 |
| `common/Result.java` | 添加 `@Schema` 注解，让所有响应结构出现在 Models 区域 |
| `controller/*` | 添加 `@Tag` / `@Operation` / `@Parameter` / `@ApiResponses` 注解 |

---

## 6. 后续推进清单（建议）

- [ ] 实体类（`entity/*.java`）补充 `@Schema` 注解，让 Models 区域更完整
- [ ] DTO / VO 与 Controller 解耦后，给请求体与返回体补充 `@Schema`
- [ ] 接入 JWT 后，把 `OpenApiConfig` 中的 `SecurityScheme` 真正生效（全局 Bearer）
- [ ] CI 中加一步：`curl /api/v3/api-docs > docs/api/openapi.json`，自动随版本固化
- [ ] Knife4j 4.x 支持接口**分组与版本**切换，可在 `OpenApiConfig` 中按需配置 `GroupedOpenApi`

---

## 7. 常见问题

**Q1：访问 `doc.html` 提示 401？**
请确认 `SecurityConfig` 已加入 `/doc.html`、`/v3/api-docs/**`、`/swagger-ui/**`、`/webjars/**` 的放行规则（本次已配置）。

**Q2：扫描不到接口？**
确认 `application.yml` 中 `springdoc.packages-to-scan` 指向 `com.legal.legislation.controller`，且 Controller 上有 `@RestController` 注解。

**Q3：团队分享时不想暴露测试服务器地址？**
把 `openapi.json` 导出后给团队成员，他们可以使用 Apifox 等工具在本地 mock 环境调试。