# 智立法 · 行政立法智能辅助平台 —— 后端部署说明

> 本文档面向**团队成员**和**运维同事**，介绍如何把后端打成 war 包，
> 部署到本地或服务器上的 **Apache Tomcat 9.x / 10.x** 上运行。
> 阅读对象：第一次接触本项目的同事。

---

## 一、5 分钟快速上手（最省事的路线）

> 适合：本地体验、给领导演示、临时跑一下。

### 1.1 准备环境

| 工具       | 版本                | 说明                                                         |
| ---------- | ------------------- | ------------------------------------------------------------ |
| JDK        | **17 或以上**       | Tomcat 10 要求 Servlet 6.0（Jakarta EE 10），必须 JDK 17     |
| Maven      | 3.8+                | 用于编译打 war 包                                            |
| Tomcat     | **10.x**（推荐）    | 也支持 Tomcat 9，但建议统一用 10.x                          |
| MySQL      | 8.0+                | 需要先建好数据库 `legal_legislation`（下文有 SQL 脚本）     |

### 1.2 准备 MySQL

打开 MySQL 客户端执行：

```sql
CREATE DATABASE legal_legislation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后导入项目里的建表脚本（**首次部署**才需要）：

```bash
# Windows PowerShell
mysql -u root -p legal_legislation < "D:\ProjectSpace\CaseGuardian\legislation_edition\backend\sql\legislation_schema.sql"
```

### 1.3 编译打 war 包

进入后端目录：

```powershell
cd D:\ProjectSpace\CaseGuardian\legislation_edition\backend

# 第一次会下载依赖，可能要 5~10 分钟，请耐心等待
mvn clean package -DskipTests
```

成功后会在 `target/` 目录生成：

```
target\legislation-edition-backend.war     ← 部署用的 war 包（约 100MB）
```

### 1.4 部署到 Tomcat（两种方法，任选其一）

#### 方法 A：扔进 webapps（最傻瓜式 ⭐ 推荐）

1. 把 `legislation-edition-backend.war` 复制到 Tomcat 的 `webapps` 目录下
2. **（可选）** 把文件重命名为 `ROOT.war`，这样访问路径就不带 war 名
3. 双击 Tomcat 安装目录下 `bin\startup.bat` 启动
4. 看到窗口输出 `Started LegislationApplication in XX seconds` 就成功了

#### 方法 B：用 Tomcat Manager（适合服务器远程管理）

1. 在 Tomcat `conf\tomcat-users.xml` 加上管理员账号：

   ```xml
   <role rolename="manager-gui"/>
   <user username="admin" password="你的密码" roles="manager-gui"/>
   ```

2. 重启 Tomcat，访问 `http://localhost:8080/manager/html`

3. 选择「WAR file to deploy」上传刚才的 war 包

### 1.5 验证是否跑起来

打开浏览器访问：

| 用途               | 地址                                                         |
| ------------------ | ------------------------------------------------------------ |
| 主页（404 是正常） | http://localhost:8080/legislation-edition-backend/           |
| API 文档（Knife4j）⭐ | http://localhost:8080/legislation-edition-backend/doc.html  |
| OpenAPI JSON       | http://localhost:8080/legislation-edition-backend/v3/api-docs |
| 登录接口           | http://localhost:8080/legislation-edition-backend/auth/login |

> ⚠️ **路径里有 `/legislation-edition-backend/`** 是因为 war 文件名就是这个。
> 如果你把 war 重命名成了 `ROOT.war` 或者 `api.war`，路径前缀就会变成 `/` 或 `/api/`。

---

## 二、详细部署步骤（生产 / 测试服务器推荐）

### 2.1 安装 JDK 17

下载并安装：https://adoptium.net/temurin/releases/?version=17

安装完验证：

```powershell
java -version
# 应该输出 openjdk version "17.x.x"
```

### 2.2 安装 Tomcat 10

1. 下载：https://tomcat.apache.org/download-10.cgi
   - Windows 选 `64-bit Windows zip`（免安装版）
2. 解压到任意目录，比如 `D:\apache-tomcat-10.1.x`
3. **建议**：把 Tomcat 注册成 Windows 服务，开机自启
   - 进入 `bin` 目录，命令行执行 `service.bat install Tomcat10`

### 2.3 配置 Tomcat

编辑 `conf\server.xml`（可选）：

```xml
<!-- 修改默认端口 8080 为 80 / 8090 等 -->
<Connector port="8090" protocol="HTTP/1.1" ... />
```

> 注意：Tomcat 10 的 `<Connector>` 默认还是 8080，按需改。

### 2.4 配置数据库连接（推荐用外部 yml）

> **不要**每次改数据库都重新打 war 包。把 yml 放到 Tomcat 外部即可热改。

**步骤**：

1. 在 Tomcat 安装目录下新建文件夹 `conf\config\`（或任意你喜欢的位置）
2. 复制 `application-prod.yml`（项目里已提供）到该目录
3. 修改里面的数据库账号密码、jwt.secret 等
4. 设置环境变量让 Spring Boot 加载这个文件：

   **方法 1（推荐，Windows 系统环境变量）**：

   ```powershell
   # 一次性设置（PowerShell 管理员模式）
   [System.Environment]::SetEnvironmentVariable("SPRING_CONFIG_LOCATION", "D:\apache-tomcat-10.1.x\conf\config\application-prod.yml", "Machine")
   ```

   **方法 2（改 startup.bat）**：

   在 `bin\startup.bat` 的开头加一行：

   ```bat
   set "SPRING_CONFIG_LOCATION=D:\apache-tomcat-10.1.x\conf\config\application-prod.yml"
   ```

5. 重启 Tomcat 即可生效

### 2.5 部署 war 包

把 `target\legislation-edition-backend.war` 复制到 `webapps\` 目录即可。

- 第一次启动会**自动解压** war，可能需要 1~2 分钟
- 看到 `catalina.out` 或 `logs\legislation-backend.log` 里有 `Started LegislationApplication` 就 OK

### 2.6 启动 / 停止 Tomcat

```powershell
# 启动
D:\apache-tomcat-10.1.x\bin\startup.bat

# 停止
D:\apache-tomcat-10.1.x\bin\shutdown.bat
```

查看实时日志：

```powershell
# 实时查看后端日志
Get-Content D:\apache-tomcat-10.1.x\logs\legislation-backend.log -Wait
```

---

## 三、常见问题排查

### Q1：启动报 `java.lang.UnsupportedClassVersionError`

**原因**：JDK 版本太低。Spring Boot 3.x 要求 JDK 17+。

**解决**：安装 JDK 17，并确认 `JAVA_HOME` 指向 JDK 17：

```powershell
$env:JAVA_HOME
# 应输出 D:\Program Files\Eclipse Adoptium\jdk-17.x.x.x
```

### Q2：启动报 `java.lang.NoClassDefFoundError: jakarta/servlet/...`

**原因**：用了 Tomcat 9（Tomcat 9 是 `javax.servlet`，而本项目用的是 `jakarta.servlet`）。

**解决**：换成 Tomcat 10.x。

### Q3：访问 API 一直 404

**可能原因**：

1. **路径不对**：Tomcat 默认会加 war 文件名作为前缀。
   - 实际访问路径 = `http://ip:port/<war文件名>/<你的接口路径>`
   - 例子：war 名 = `legislation-edition-backend.war`，接口是 `/auth/login`
   - 那访问地址是：`http://localhost:8080/legislation-edition-backend/auth/login`

2. **数据库没建**：先确认 MySQL 里 `legal_legislation` 库和表都建好了。

3. **没启动完**：第一次启动会慢一些，1~2 分钟后再试。

**解决**：

- **方案 A**：把 war 重命名成 `ROOT.war` 部署 → 访问路径就没有前缀
- **方案 B**：在 `conf\server.xml` 的 `<Host>` 节点下加：

  ```xml
  <Context path="" docBase="legislation-edition-backend" reloadable="true" />
  ```

  路径就变成 `http://localhost:8080/auth/login`

### Q4：Knife4j 文档页面打开是空白

**原因**：可能 Knife4j 静态资源被 Spring Security 拦截了。

**解决**：本项目 SecurityConfig 已放行 `/doc.html`、`/webjars/**` 等路径，重启 Tomcat 即可。

如果还是不行，**清浏览器缓存** 或 **F12 → Network → Disable cache** 重试。

### Q5：上传大文件（>50MB）失败

修改 `application-prod.yml`：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 200MB
      max-request-size: 200MB
```

同时修改 Tomcat 的 `conf\server.xml`：

```xml
<Connector port="8090" protocol="HTTP/1.1"
           maxPostSize="209715200" />   <!-- 200MB，单位字节 -->
```

### Q6：如何看版本号 / 接口调用次数

- 版本号：`target\legislation-edition-backend.war\WEB-INF\classes\application.yml` 顶部
- 接口调用统计：访问 Knife4j 文档左下角「接口统计」

---

## 四、目录结构速查

```
D:\ProjectSpace\CaseGuardian\legislation_edition\backend\
├── pom.xml                                    # Maven 配置
├── src\main\java\                             # Java 源码
├── src\main\resources\
│   ├── application.yml                        # 默认配置（开发用）
│   ├── application-prod.yml                   # 生产配置（部署时复制到外部）
│   ├── mapper\                                # MyBatis XML
│   └── db\migration\                          # Flyway 迁移脚本
├── sql\
│   └── legislation_schema.sql                 # 首次部署用的建表 SQL
└── target\
    └── legislation-edition-backend.war        # 打出来的部署包 ⭐
```

---

## 五、团队成员速查表

| 我想要…                  | 操作                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 第一次把项目跑起来       | 看 **第一章 5 分钟快速上手**                                 |
| 改数据库地址             | 改 Tomcat 外部的 `application-prod.yml`，**不要**改源码       |
| 改启动端口               | 改 Tomcat 的 `conf\server.xml` 里 `<Connector port="...">`   |
| 重新打 war 包            | `mvn clean package -DskipTests`                              |
| 看日志                   | `D:\apache-tomcat-10.1.x\logs\legislation-backend.log`       |
| 重启服务                 | `shutdown.bat` → `startup.bat`                               |
| 接口文档（中文 UI）      | http://localhost:8080/legislation-edition-backend/doc.html   |
| 报错了怎么办             | 复制完整错误信息发群里 @ 后端开发                            |

---

## 六、版本信息

- **Spring Boot**：3.1.6
- **JDK**：17+
- **Tomcat**：10.x（推荐 10.1.x）
- **MySQL**：8.0+
- **项目维护**：智立法研发组

> 文档最后更新：2026-10-03
