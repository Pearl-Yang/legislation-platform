#!/usr/bin/env bash
# =============================================================================
# 智立法 - 一键导出 OpenAPI 3 JSON 文档
#
# 不需要 MySQL / Neo4j,使用 H2 内存数据库 + openapi profile 跑 30 秒即可。
#
# 用法:
#   chmod +x scripts/export-openapi.sh
#   ./scripts/export-openapi.sh                    # 输出到 ./openapi.json
#   OUTPUT_PATH=./docs/openapi.json ./scripts/export-openapi.sh
#
# 适用 CI: GitHub Actions / GitLab CI 等任意 Linux 容器
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/../legislation-edition/backend"

OUTPUT_PATH="${OUTPUT_PATH:-${SCRIPT_DIR}/../openapi.json}"
DOCS_BASE="${DOCS_BASE:-http://127.0.0.1:8083}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: mvn 未安装"
  exit 1
fi

cd "${BACKEND_DIR}"
echo "[export-openapi] 构建 jar ..."
mvn -q -DskipTests package -o >/dev/null 2>&1 || mvn -q -DskipTests package

JAR_FILE=$(ls target/legislation-edition-backend.jar 2>/dev/null | head -1 || true)
if [ -z "${JAR_FILE}" ]; then
  echo "ERROR: jar not built"
  exit 1
fi

echo "[export-openapi] 启动后端 (openapi profile) ..."
SPRING_PROFILES_ACTIVE=openapi \
  java -jar "${JAR_FILE}" \
    --server.port=8083 \
    --server.servlet.context-path=/api \
    > /tmp/openapi-export.log 2>&1 &

PID=$!
trap "kill -9 ${PID} 2>/dev/null || true" EXIT

# 等待就绪(actuator/health)
for i in $(seq 1 60); do
  if curl -sf "${DOCS_BASE}/actuator/health" >/dev/null 2>&1; then
    echo "[export-openapi] backend ready"
    break
  fi
  sleep 1
done

# 导出 openapi.json
mkdir -p "$(dirname "${OUTPUT_PATH}")"
curl -sf "${DOCS_BASE}/v3/api-docs" -o "${OUTPUT_PATH}"
SIZE=$(stat -c%s "${OUTPUT_PATH}" 2>/dev/null || stat -f%z "${OUTPUT_PATH}")
echo "[export-openapi] saved to ${OUTPUT_PATH} (${SIZE} bytes)"

# 也导出一份 yaml(便于 Knife4j / ReDoc / 文档站使用)
YAML_PATH="${OUTPUT_PATH%.json}.yaml"
curl -sf "${DOCS_BASE}/v3/api-docs.yaml" -o "${YAML_PATH}" 2>/dev/null || true

echo "[export-openapi] done"