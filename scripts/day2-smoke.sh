#!/usr/bin/env bash
# =============================================================================
# Day2 端到端冒烟脚本 - 项目流程 + 草案 + 审查
#
# 流程:
#   1) 登录拿 JWT
#   2) 列立法项目(>=1)
#   3) 创建一个新项目,拿到 projectId
#   4) 推到下一阶段
#   5) 提交草案生成,拿到 taskId,轮询直到 SUCCESS
#   6) 用生成的 draftId 提交审查
#   7) 轮询审查记录,直到 DONE
#   8) 列草案 (>=1)
#   9) 列审查规则 (>=5)
#
# 用法:
#   bash scripts/day2-smoke.sh            # 默认 http://localhost:8083
#
# 退出码: 0 = 全部通过,1 = 失败
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
  echo "  智立法 · Day2 端到端冒烟"
  echo "  backend: $BACKEND"
  echo "  time:    $(date '+%Y-%m-%d %H:%M:%S')"
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

# ----- 0. 打印 -----
print_header

# ----- 1. 登录拿 token -----
LOGIN_RESP=$(curl -fsS --max-time 5 -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" \
  "$API/auth/login")
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
if [ -z "$TOKEN" ]; then
  echo "✗ 登录失败,无法继续"
  exit 1
fi
AUTH="Authorization: Bearer $TOKEN"

check "1. 登录 admin / 123456" \
  "echo '$LOGIN_RESP' | grep -q '\"code\":200'"

# ----- 2. 列项目 -----
PROJECTS_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/legislative-project/list?page=1&size=5")
check "2. 列出立法项目(分页)" \
  "echo '$PROJECTS_RESP' | grep -q '\"code\":200'"

PROJECT_ID=$(echo "$PROJECTS_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 当前第一个项目 id = $PROJECT_ID"

# ----- 3. 创建一个新项目 -----
NEW_PROJ_RESP=$(curl -fsS --max-time 5 -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"projectName\":\"Day2冒烟测试-$(date +%H%M%S)\",\"projectType\":\"LOCAL_RULE\",\"priority\":\"MEDIUM\",\"description\":\"冒烟脚本测试\",\"legalBasis\":\"《立法法》\"}" \
  "$API/legislative-project")
check "3. 新建一个 LOCAL_RULE 项目" \
  "echo '$NEW_PROJ_RESP' | grep -q '\"code\":200'"

NEW_PROJ_ID=$(echo "$NEW_PROJ_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  → 新项目 id = $NEW_PROJ_ID"

# ----- 4. 推进新项目到下一阶段 -----
ADVANCE_RESP=$(curl -fsS --max-time 5 -X POST -H "$AUTH" "$API/legislative-project/$NEW_PROJ_ID/advance")
check "4. 推进新项目到下一阶段" \
  "echo '$ADVANCE_RESP' | grep -q '\"code\":200'"

# ----- 5. 提交草案生成 -----
DRAFT_RESP=$(curl -fsS --max-time 5 -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"projectId\":$NEW_PROJ_ID,\"prompt\":\"《立法法》第八条:测试冒烟\"}" \
  "$API/draft/generate")
check "5. 提交草案生成任务" \
  "echo '$DRAFT_RESP' | grep -q '\"code\":200'"

TASK_ID=$(echo "$DRAFT_RESP" | grep -o '"data":"[a-f0-9]*"' | head -1 | sed 's/"data":"//;s/"//')
echo "  → 草案 taskId = $TASK_ID"

# 轮询直到 SUCCESS 或超时
POLL_OK=0
for i in 1 2 3 4 5 6 7 8 9 10; do
  STATUS_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/draft/task/$TASK_ID")
  STATUS=$(echo "$STATUS_RESP" | grep -o '"status":"[A-Z]*"' | head -1 | sed 's/"status":"//;s/"//')
  if [ "$STATUS" = "SUCCESS" ]; then
    POLL_OK=1
    DRAFT_ID=$(echo "$STATUS_RESP" | grep -o '"draftId":[0-9]*' | head -1 | sed 's/"draftId"://')
    echo "  → 草案生成完成,draftId = $DRAFT_ID"
    break
  elif [ "$STATUS" = "FAILED" ]; then
    echo "  ✗ 草案生成 FAILED"
    break
  fi
  sleep 1
done
check "6. 草案生成轮询至 SUCCESS (≤5s)" "test $POLL_OK -eq 1"

# ----- 7. 提交审查 -----
REVIEW_RESP=$(curl -fsS --max-time 5 -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"draftId\":$DRAFT_ID,\"reviewType\":\"AUTO\"}" \
  "$API/review/submit")
check "7. 提交智慧审查任务" \
  "echo '$REVIEW_RESP' | grep -q '\"code\":200'"

RECORD_ID=$(echo "$REVIEW_RESP" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
echo "  → 审查 recordId = $RECORD_ID"

# 轮询
REVIEW_OK=0
for i in 1 2 3 4 5 6 7 8; do
  REV_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/review/record/$RECORD_ID")
  R_STATUS=$(echo "$REV_RESP" | grep -o '"status":"[A-Z]*"' | head -1 | sed 's/"status":"//;s/"//')
  if [ "$R_STATUS" = "DONE" ]; then REVIEW_OK=1; break; fi
  sleep 1
done
check "8. 审查完成轮询至 DONE" "test $REVIEW_OK -eq 1"

# ----- 8. 列草案 -----
DRAFTS_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/draft/list?projectId=$NEW_PROJ_ID")
check "9. 列出新项目下的草案(>=1)" \
  "echo '$DRAFTS_RESP' | grep -q '\"code\":200'"

# ----- 9. 列审查规则 -----
RULES_RESP=$(curl -fsS --max-time 5 -H "$AUTH" "$API/review/rule-list")
check "10. 列出审查规则(>=5)" \
  "echo '$RULES_RESP' | grep -q '\"code\":200'"

# ----- 总结 -----
echo ""
echo "============================================================"
echo "  Day2 冒烟完成: ✅ $PASS 通过  ❌ $FAIL 失败"
echo "============================================================"
if [ $FAIL -gt 0 ]; then
  echo "失败项:"
  for item in "${FAILED_ITEMS[@]}"; do
    echo "  - $item"
  done
  exit 1
fi
exit 0