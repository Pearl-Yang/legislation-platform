#!/usr/bin/env bash
# =============================================================================
# 智立法 · Qwen 在线 API 真路径验证脚本
# 用途:在没起 docker / 没起后端的情况下,直接 curl 一次 DashScope,
#       确认 API Key 有效 + 返回 > 1000 字
#
# 用法:
#   export DASHSCOPE_API_KEY="sk-xxx"        # 或 source ../../.env
#   bash scripts/qwen-online-verify.sh
#
# 成功标准:
#   - HTTP 200
#   - choices[0].message.content 长度 >= 800
#   - 退出码 0
# 失败:
#   - HTTP 非 200 / content 长度不够 → 退出码 1,日志提示检查 Key 或网络
# =============================================================================

set -e

# ---------- 1. 加载 API Key ----------
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$QWEN_API_KEY" ]; then
  if [ -f "$(dirname "$0")/../.env" ]; then
    echo "[Qwen-verify] 从 .env 加载 DASHSCOPE_API_KEY"
    set -a
    # shellcheck disable=SC1091
    source "$(dirname "$0")/../.env"
    set +a
  else
    echo "[Qwen-verify][ERROR] 未找到 API Key,请先 export DASHSCOPE_API_KEY=sk-xxx 或在 .env 中配置"
    exit 1
  fi
fi

KEY="${QWEN_API_KEY:-$DASHSCOPE_API_KEY}"
BASE_URL="${QWEN_BASE_URL:-https://dashscope.aliyuncs.com/compatible-mode}"
MODEL="${QWEN_MODEL:-qwen-plus}"

if [ -z "$KEY" ]; then
  echo "[Qwen-verify][ERROR] API Key 为空,无法调用"
  exit 1
fi

# 脱敏,只显示前 8 + 后 4
MASK="${KEY:0:8}...${KEY: -4}"
echo "[Qwen-verify] 准备调用 DashScope, key=$MASK model=$MODEL baseUrl=$BASE_URL"

# ---------- 2. 准备 payload ----------
PAYLOAD=$(cat <<'EOF'
{
  "model": "__MODEL__",
  "messages": [
    {"role": "system", "content": "你是一位资深的行政立法起草专家,熟悉《行政许可法》《行政处罚法》《行政强制法》及各部门规章。请用规范、严谨、简明的立法语言回答。"},
    {"role": "user", "content": "请起草一份《XX市电动自行车管理条例(草案)》的'总则'章节,要求:\n1. 立法目的与依据(含《道路交通安全法》《产品质量法》等上位法)\n2. 适用范围(本市行政区域内)\n3. 基本原则(合法、科学、民主、公开)\n4. 部门职责(公安交管、市场监管、住建、消防等)\n5. 不少于 6 条具体条款\n输出 Markdown 格式,含'第X条'编号。"}
  ],
  "temperature": 0.3,
  "top_p": 0.9,
  "max_tokens": 2048,
  "stream": false
}
EOF
)
PAYLOAD=${PAYLOAD/__MODEL__/$MODEL}

# ---------- 3. 调用 ----------
RESP_FILE=$(mktemp)
HTTP_CODE=$(curl -sS -o "$RESP_FILE" -w "%{http_code}" \
  -X POST "$BASE_URL/v1/chat/completions" \
  -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" 2>&1) || {
    echo "[Qwen-verify][ERROR] curl 失败: $HTTP_CODE"
    rm -f "$RESP_FILE"
    exit 1
  }

echo "[Qwen-verify] HTTP 状态码: $HTTP_CODE"
if [ "$HTTP_CODE" != "200" ]; then
  echo "[Qwen-verify][ERROR] HTTP 非 200,响应体:"
  cat "$RESP_FILE"
  rm -f "$RESP_FILE"
  exit 1
fi

# ---------- 4. 解析 content 长度 ----------
# 用 python 做 JSON 解析(避免 jq 依赖)
if command -v python >/dev/null 2>&1; then
  CONTENT=$(python -c "
import json,sys
d=json.load(open('$RESP_FILE'))
c=d.get('choices',[{}])[0].get('message',{}).get('content','')
print(c)
" 2>/dev/null)
  LEN=${#CONTENT}
  echo "[Qwen-verify] 返回 content 长度: $LEN 字符"
  if [ "$LEN" -lt 800 ]; then
    echo "[Qwen-verify][WARN] content 长度 < 800,可能 Key 配额受限或模型降级"
    echo "--- 前 500 字 ---"
    echo "$CONTENT" | head -c 500
    echo
    rm -f "$RESP_FILE"
    exit 1
  fi
  echo "--- 前 600 字预览 ---"
  echo "$CONTENT" | head -c 600
  echo
  echo "..."
  echo "--- 后 400 字 ---"
  echo "$CONTENT" | tail -c 400
  echo
else
  echo "[Qwen-verify] 未找到 python,只打印原始响应"
  cat "$RESP_FILE" | head -c 2000
  echo
fi

# ---------- 5. 落盘 + 退出 ----------
OUT_FILE="$(dirname "$0")/../docker/qwen-online-verify-result.json"
mkdir -p "$(dirname "$OUT_FILE")"
cp "$RESP_FILE" "$OUT_FILE" 2>/dev/null || true
rm -f "$RESP_FILE"

echo "[Qwen-verify] ✅ 通过(响应已保存到 $OUT_FILE)"
exit 0
