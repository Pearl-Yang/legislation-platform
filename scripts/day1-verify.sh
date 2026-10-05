#!/usr/bin/env bash
# =============================================================================
# Day1 启动验证脚本 - 智立法 · 行政立法智能辅助平台
#
# 用途:
#   1) 验证后端 /api/auth/health 是否可达
#   2) 验证 Dashboard 三个核心数据接口能返回数据(非 500 / 非 401)
#   3) 验证 5 个种子账号能登录拿到 JWT
#   4) 验证 Grafana / Prometheus / Knife4j 文档可达
#
# 用法:
#   bash scripts/day1-verify.sh            # 用默认 http://localhost:8083
#   BACKEND=http://10.0.0.5:8083 bash scripts/day1-verify.sh
#
# 退出码:
#   0 = 全部通过
#   1 = 至少 1 项失败(打印失败项)
# =============================================================================

set -u

BACKEND="${BACKEND:-http://localhost:8083}"
API="$BACKEND/api"
PASS=0
FAIL=0
FAILED_ITEMS=()

print_header() {
  echo ""
  echo "============================================================"
  echo "  智立法 · Day1 启动验证"
  echo "  backend: $BACKEND"
  echo "  time:    $(date '+%Y-%m-%d %H:%M:%S')"
  echo "============================================================"
}

check() {
  local name="$1"; shift
  local cmd_to_run="$*"
  echo ""
  echo "▶ $name"
  if eval "$cmd_to_run"; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1))
    FAILED_ITEMS+=("$name")
  fi
}

# ===== 1. 后端健康检查 =====
check "后端 /api/auth/health" \
  "curl -fsS --max-time 5 '$API/auth/health' | grep -q '\"status\":\"UP\"'"

# ===== 2. Dashboard 三个核心数据接口 =====
check "GET /api/info/dashboard 返回 code === 200" \
  "curl -fsS --max-time 5 '$API/info/dashboard' | grep -q '\"code\":200'"

check "GET /api/info/dashboard/chart 返回 code === 200" \
  "curl -fsS --max-time 5 '$API/info/dashboard/chart' | grep -q '\"code\":200'"

check "GET /api/info/map/regulation 返回 code === 200" \
  "curl -fsS --max-time 5 '$API/info/map/regulation' | grep -q '\"code\":200'"

# ===== 3. 5 个种子账号登录 =====
for U in admin leader drafter reviewer evaluator; do
  check "种子账号 $U / 123456 登录" \
    "curl -fsS --max-time 5 -H 'Content-Type: application/json' \
        -d '{\"username\":\"$U\",\"password\":\"123456\"}' \
        '$API/auth/login' | grep -q '\"code\":200'"
done

# ===== 4. Knife4j 文档 =====
check "Knife4j 文档可达" \
  "curl -fsSI --max-time 5 '$API/doc.html' | head -n1 | grep -q '200'"

# ===== 5. Prometheus metrics =====
check "Prometheus 抓取端点 /actuator/prometheus" \
  "curl -fsS --max-time 5 '$BACKEND/actuator/prometheus' | grep -q 'jvm_memory_used_bytes'"

# ===== 总结 =====
echo ""
echo "============================================================"
echo "  验证完成: ✅ $PASS 通过  ❌ $FAIL 失败"
echo "============================================================"
if [ $FAIL -gt 0 ]; then
  echo "失败项:"
  for item in "${FAILED_ITEMS[@]}"; do
    echo "  - $item"
  done
  exit 1
fi
exit 0