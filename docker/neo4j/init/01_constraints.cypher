// =====================================================================
// 智立法 - Neo4j 约束 / 索引初始化
// 启动 Neo4j 容器时,Dockerfile 把此文件挂载到 /var/lib/neo4j/import/
// 容器首次启动会自动执行 /docker-entrypoint-initdb.d/ 下的所有 .cypher / .sh
// =====================================================================

// ----- 法规节点 -----
CREATE CONSTRAINT regulation_id IF NOT EXISTS
  FOR (r:Regulation) REQUIRE r.id IS UNIQUE;

CREATE INDEX regulation_name IF NOT EXISTS
  FOR (r:Regulation) ON (r.name);

CREATE INDEX regulation_type IF NOT EXISTS
  FOR (r:Regulation) ON (r.type);

CREATE INDEX regulation_region IF NOT EXISTS
  FOR (r:Regulation) ON (r.region_code);

CREATE INDEX regulation_status IF NOT EXISTS
  FOR (r:Regulation) ON (r.status);

// ----- 法条节点(预留,Phase 2 用) -----
CREATE CONSTRAINT article_id IF NOT EXISTS
  FOR (a:Article) REQUIRE a.id IS UNIQUE;

CREATE INDEX article_regulation IF NOT EXISTS
  FOR (a:Article) ON (a.regulation_id);

CREATE INDEX article_index IF NOT EXISTS
  FOR (a:Article) ON (a.article_index);

// ----- 立法项目节点(预留) -----
CREATE CONSTRAINT project_id IF NOT EXISTS
  FOR (p:Project) REQUIRE p.id IS UNIQUE;

CREATE INDEX project_type IF NOT EXISTS
  FOR (p:Project) ON (p.type);

// ----- 关系类型约束(Neo4j 5.x 支持) -----
// CITES      引用
// SUBORDINATE 下位
// SUPERSEDE  替代
// REPEAL     废止
// REFERENCE  参照
// 暂不强制要求端点类型,留给 Phase 2 接入 RAG 时再加
