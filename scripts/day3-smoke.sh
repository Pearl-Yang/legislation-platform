#!/usr/bin/env bash
# =============================================================================
# Day3 端到端冒烟脚本 - 评估 + 清理 + 意见征集
# 用法: bash scripts/day3-smoke.sh
# =============================================================================

set -u

BACKEND="${BACKEND:-http://localhost:8083}"
API="$BACKEND/api"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-123456}"
PASS=0
FAIL=0
FAILED_ITEMS=()

print_header () {
  echo "============================================================"
  echo "  智立法 · Day3 端到端冒烟"
  echo "  backend: $BACKEND"
  echo "============================================================"
}

check () {
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

print_header

# ----- 1. 登录 -----
LOGIN_RESP=$(curl -fsS --max-time 5 -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" "$API/auth/login")
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
[ -z "$TOKEN" ] && { echo "✗ 登录失败"; exit 1; }
AUTH="Authorization: Bearer $TOKEN"
check "1. 登录 $USERNAME / $PASSWORD" "echo '$LOGIN_RESP' | grep -q '\"code\":200'"

# ----- 2. 评估:列出任务 -----
EVAL_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/evaluation/list")
check "2. 列出评估任务" "echo '$EVAL_RESP' | grep -q '\"code\":200'"

# 提取第一个 evaluation id
EVAL_ID=$(echo "$EVAL_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 当前 evaluation id = $EVAL_ID"

# ----- 3. 评估图表 -----
if [ -n "$EVAL_ID" ]; then
  check "3. 拉取评估图表数据" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/evaluation/$EVAL_ID/chart-data\" | grep -q '\"code\":200'"
else
  echo "  ⚠ 无评估任务,跳过图表接口"
fi

# ----- 4. 评估指标 -----
check "4. 列出评估指标" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/evaluation/indicator-list\" | grep -q '\"code\":200'"

# ----- 5. 清理:列出任务 -----
CLEANUP_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/cleanup/task/list")
check "5. 列出清理任务" "echo '$CLEANUP_RESP' | grep -q '\"code\":200'"

CLEANUP_ID=$(echo "$CLEANUP_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 当前 cleanup id = $CLEANUP_ID"

# ----- 6. 清理受影响法规 -----
if [ -n "$CLEANUP_ID" ]; then
  check "6. 列出清理任务候选法规" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/cleanup/task/$CLEANUP_ID/affected-regulations\" | grep -q '\"code\":200'"
fi

# ----- 7. 意见征集:列出 -----
CONS_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/consultation/list?page=1&size=10")
check "7. 列出意见征集" "echo '$CONS_RESP' | grep -q '\"code\":200'"

CONS_ID=$(echo "$CONS_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 当前 consultation id = $CONS_ID"

# ----- 8. 意见征集:详情 -----
if [ -n "$CONS_ID" ]; then
  check "8. 意见征集详情" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/consultation/$CONS_ID\" | grep -q '\"code\":200'"

  check "9. 意见征集意见列表" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/consultation/$CONS_ID/opinions\" | grep -q '\"code\":200'"

  check "10. 意见征集统计" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/consultation/$CONS_ID/statistics\" | grep -q '\"code\":200'"

  check "11. 意见征集词云" \
    "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/consultation/$CONS_ID/wordcloud\" | grep -q '\"code\":200'"
fi

# ----- 12. Dashboard 8 个核心接口 -----
check "12. /info/dashboard 主聚合" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/dashboard\" | grep -q '\"code\":200'"

check "13. /info/dashboard/chart 月度趋势" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/dashboard/chart\" | grep -q '\"code\":200'"

check "14. /info/map/regulation 地区分布" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/map/regulation\" | grep -q '\"code\":200'"

check "15. /legislative-project/dashboard 项目仪表盘" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/legislative-project/dashboard\" | grep -q '\"code\":200'"

check "16. /legislative-project/upcoming 即将到期" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/legislative-project/upcoming?days=30&limit=5\" | grep -q '\"code\":200'"

echo ""
echo "============================================================"
echo "  Day3 冒烟完成: ✅ $PASS 通过  ❌ $FAIL 失败"
echo "============================================================"
if [ $FAIL -gt 0 ]; then
  echo "失败项:"
  for item in "${FAILED_ITEMS[@]}"; do
    echo "  - $item"
  done
  exit 1
fi
exit 0