# Docker 部署说明

## 目录结构

```
docker/
├── docker-compose.yml         # 一键起 MySQL + Neo4j + Backend
├── backend.Dockerfile         # 后端镜像
├── neo4j/
│   ├── init/                  # 首次启动自动执行
│   │   └── 01_constraints.cypher
│   └── conf/                  # 自定义 neo4j.conf(可选)
└── README.md                  # 本文件
```

## 快速开始

```bash
# 1) 准备 .env
cp .env.example .env
# 编辑 .env 填入密码 / JWT secret / DashScope Key(可选)

# 2) 启动
cd docker
docker compose up -d

# 3) 看后端启动日志(等待 DataInitializer 跑完)
docker compose logs -f backend

# 4) 验证
curl http://localhost:8083/api/auth/health

# 5) 登录
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

## 数据持久化

- MySQL 数据: `docker-data/mysql/`
- Neo4j 数据: `docker-data/neo4j/`

首次 `docker compose down` 不会丢数据;带 `-v` 才会清空。

## Neo4j 关闭方式(节省内存)

如果暂时不用图谱,把 `.env` 里的 `NEO4J_ENABLED=false`,同时:

```bash
docker compose stop neo4j
```

需要时再 `docker compose start neo4j`。

## 端口占用

| 服务 | 端口 |
|------|------|
| MySQL | 3306 |
| Neo4j HTTP | 7474 |
| Neo4j Bolt | 7687 |
| Backend | 8083 |

修改 `.env` 中 `DB_PORT` 可改 MySQL 端口。

## 不使用 Docker 的情况

参考 `legislation-edition/backend/README.md`,手动:
- 安装 MySQL 8.0
- (可选)安装 Neo4j 5.x
- 执行 `mysql -uroot -p < legislation-edition/backend/sql/legislation_schema.sql`
- `cd legislation-edition/backend && mvn spring-boot:run`
