#!/usr/bin/env bash
# =============================================================================
# Day1 启动验证脚本 - 智立法 · 行政立法智能辅助平台
# 跨平台稳定版:避免 PowerShell + bash 的引号转义问题
#
# 用途:
#   1) 验证后端 /api/auth/health 是否可达
#   2) 验证 Dashboard 三个核心数据接口能返回数据(需 JWT)
#   3) 验证 5 个种子账号能登录拿到 JWT
#   4) 验证 Grafana / Prometheus / Knife4j 文档可达
#
# 用法:
#   bash scripts/day1-verify.sh            # 默认 http://localhost:8083
#   BACKEND=http://10.0.0.5:8083 bash scripts/day1-verify.sh
#
# 退出码:
#   0 = 全部通过
#   1 = 至少 1 项失败
# =============================================================================

set -u

BACKEND="${BACKEND:-http://localhost:8083}"
API="$BACKEND/api"
PASS=0
FAIL=0
FAILED_ITEMS=()

# 用临时文件传 JSON,避免 PowerShell 吃掉引号
TMPDIR_LOCAL=$(mktemp -d 2>/dev/null || mktemp -d -t 'legislation')
trap 'rm -rf "$TMPDIR_LOCAL"' EXIT

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
  if eval "$cmd_to_run" 2>/dev/null; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1))
    FAILED_ITEMS+=("$name")
  fi
}

# JSON 写入临时文件,避免 shell 引号问题
write_json_payload() {
  local user="$1"
  local file="$TMPDIR_LOCAL/login_${user}.json"
  printf '{"username":"%s","password":"123456"}' "$user" > "$file"
  echo "$file"
}

# 用 python 提取 token(更稳)
extract_token() {
  local resp_file="$1"
  python3 -c "
import json,sys
try:
    d=json.load(open('$resp_file'))
    print(d.get('data',{}).get('token','') or '')
except Exception:
    print('')
" 2>/dev/null
}

# ===== 1. 后端健康检查 =====
check "后端 /api/auth/health" \
  "curl -fsS --max-time 5 '$API/auth/health' | grep -q '\"status\":\"UP\"'"

# ===== 2. 登录拿 JWT =====
ADMIN_PAYLOAD=$(write_json_payload admin)
ADMIN_RESP="$TMPDIR_LOCAL/login_admin_resp.json"
curl -fsS --max-time 5 -H "Content-Type: application/json" \
  --data-binary "@$ADMIN_PAYLOAD" \
  "$API/auth/login" -o "$ADMIN_RESP" 2>/dev/null
TOKEN=$(extract_token "$ADMIN_RESP")

if [ -z "$TOKEN" ]; then
  echo "❌ 无法获取 JWT,跳过 Dashboard 接口验证"
  FAIL=$((FAIL+3))
  FAILED_ITEMS+=("GET /api/info/dashboard (无 JWT)")
  FAILED_ITEMS+=("GET /api/info/dashboard/chart (无 JWT)")
  FAILED_ITEMS+=("GET /api/info/map/regulation (无 JWT)")
else
  # ===== 3. Dashboard 三个核心数据接口 =====
  check "GET /api/info/dashboard 返回 code === 200" \
    "curl -fsS --max-time 5 -H 'Authorization: Bearer $TOKEN' '$API/info/dashboard' | grep -q '\"code\":200'"

  check "GET /api/info/dashboard/chart 返回 code === 200" \
    "curl -fsS --max-time 5 -H 'Authorization: Bearer $TOKEN' '$API/info/dashboard/chart' | grep -q '\"code\":200'"

  check "GET /api/info/map/regulation 返回 code === 200" \
    "curl -fsS --max-time 5 -H 'Authorization: Bearer $TOKEN' '$API/info/map/regulation' | grep -q '\"code\":200'"
fi

# ===== 4. 5 个种子账号登录 =====
for U in admin leader drafter reviewer evaluator; do
  PAYLOAD_FILE=$(write_json_payload "$U")
  RESP_FILE="$TMPDIR_LOCAL/login_${U}_resp.json"
  check "种子账号 $U / 123456 登录" \
    "curl -fsS --max-time 5 -H 'Content-Type: application/json' --data-binary '@/${PAYLOAD_FILE#/}' '$API/auth/login' -o '$RESP_FILE' && grep -q '\"code\":200' '$RESP_FILE'"
done

# ===== 5. Knife4j 文档 =====
check "Knife4j 文档可达" \
  "curl -fsSI --max-time 5 '$API/doc.html' | head -n1 | grep -q '200'"

# ===== 6. Prometheus metrics(注意:context-path=/api) =====
check "Prometheus 抓取端点 /api/actuator/prometheus" \
  "curl -fsS --max-time 5 '$BACKEND/api/actuator/prometheus' | grep -q 'jvm_memory_used_bytes'"

# ===== 7. Grafana 可达 =====
check "Grafana 可达 http://localhost:3000" \
  "curl -fsSI --max-time 5 'http://localhost:3000/login' | head -n1 | grep -qE '200|302'"

# ===== 总结 =====
echo ""
echo "============================================================"
echo "  验证完成: PASS=$PASS / FAIL=$FAIL"
echo "============================================================"
if [ $FAIL -gt 0 ]; then
  echo "失败项:"
  for item in "${FAILED_ITEMS[@]}"; do
    echo "  - $item"
  done
  exit 1
fi
exit 0
