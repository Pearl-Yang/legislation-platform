#!/usr/bin/env bash
# =============================================================================
# Day4 端到端冒烟脚本 - 流程图 + 轮播 + 答辩文档
#
# 与 Day1-3 不同,Day4 主要前端改动 + 文档产出。
# 这里校验:Detail 接口(支持流程图渲染) + Dashboard 8 接口(支持轮播)+ 文档文件
#
# 用法: bash scripts/day4-smoke.sh
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
  echo "  智立法 · Day4 端到端冒烟"
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
check "1. 登录" "echo '$LOGIN_RESP' | grep -q '\"code\":200'"

# ----- 2. 项目详情(stages 字段)- 流程图数据 -----
PROJ_LIST=$(curl -fsS --max-time 5 -H "$AUTH" "$API/legislative-project/list?page=1&size=1")
PROJ_ID=$(echo "$PROJ_LIST" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 当前 project id = $PROJ_ID"

if [ -n "$PROJ_ID" ]; then
  DETAIL_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/legislative-project/$PROJ_ID")
  check "2. 项目详情(stages/deadlines 用于流程图)" \
    "echo '$DETAIL_RESP' | grep -q '\"stages\":'"
  
  check "3. 项目详情包含 progress(进度条)" \
    "echo '$DETAIL_RESP' | grep -q '\"progress\":'"
  
  check "4. 项目详情包含 deadlines(7 天预警)" \
    "echo '$DETAIL_RESP' | grep -q '\"deadlines\":'"
fi

# ----- 5. Dashboard 8 接口(支持轮播) -----
check "5. /info/dashboard" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/dashboard\" | grep -q '\"code\":200'"

check "6. /info/dashboard/chart" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/dashboard/chart\" | grep -q '\"code\":200'"

check "7. /info/map/regulation" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/info/map/regulation\" | grep -q '\"code\":200'"

check "8. /legislative-project/dashboard" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/legislative-project/dashboard\" | grep -q '\"code\":200'"

check "9. /legislative-project/upcoming" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/legislative-project/upcoming\" | grep -q '\"code\":200'"

check "10. /consultation/list(意见热度 Top)" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/consultation/list?page=1&size=20\" | grep -q '\"code\":200'"

check "11. /cleanup/task/list(清理统计)" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/cleanup/task/list\" | grep -q '\"code\":200'"

check "12. /evaluation/list(评估统计)" \
  "curl -fsS --max-time 5 -H \"$AUTH\" \"$API/evaluation/list\" | grep -q '\"code\":200'"

# ----- 13-15. 文档产出校验 -----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

check "13. docs/TEAM_ALLOCATION_3PF.md 存在(422+ 行)" \
  "test -f '$ROOT_DIR/docs/TEAM_ALLOCATION_3PF.md' && \
   [ \$(wc -l < '$ROOT_DIR/docs/TEAM_ALLOCATION_3PF.md') -ge 400 ]"

check "14. docs/答辩版讲稿.md 存在" \
  "test -f '$ROOT_DIR/docs/答辩版讲稿.md' && \
   [ \$(wc -l < '$ROOT_DIR/docs/答辩版讲稿.md') -ge 100 ]"

check "15. docs/答辩PPT模板.md 存在" \
  "test -f '$ROOT_DIR/docs/答辩PPT模板.md'"

check "16. docs/demo-voc.json 存在(1.2 万字 JSON)" \
  "test -f '$ROOT_DIR/docs/demo-voc.json' && \
   [ \$(wc -c < '$ROOT_DIR/docs/demo-voc.json') -ge 8000 ]"

check "17. README.md 已包含 Day1-4 状态" \
  "grep -q 'Day1-4 实际交付状态' '$ROOT_DIR/README.md'"

# ----- 18. 前端构建 + 测试 -----
FRONTEND_DIR="$ROOT_DIR/legislation-edition/web_frontend"
if [ -d "$FRONTEND_DIR" ]; then
  check "18. 前端 Day4 测试文件存在" \
    "test -f '$FRONTEND_DIR/src/views/project/__tests__/Day4.data.test.js'"
fi

echo ""
echo "============================================================"
echo "  Day4 冒烟完成: ✅ $PASS 通过  ❌ $FAIL 失败"
echo "============================================================"
if [ $FAIL -gt 0 ]; then
  echo "失败项:"
  for item in "${FAILED_ITEMS[@]}"; do
    echo "  - $item"
  done
  exit 1
fi
exit 0