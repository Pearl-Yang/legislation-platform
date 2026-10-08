@echo off
REM =============================================================================
REM 智立法 · Qwen 在线 API 真路径验证脚本(Windows 批处理)
REM
REM 用法:
REM   set DASHSCOPE_API_KEY=sk-xxx
REM   scripts\qwen-online-verify.bat
REM =============================================================================

if "%DASHSCOPE_API_KEY%"=="" if "%QWEN_API_KEY%"=="" (
  if exist "%~dp0..\.env" (
    echo [Qwen-verify] 从 .env 加载 DASHSCOPE_API_KEY
    for /f "usebackq tokens=1,2 delims==" %%a in ("%~dp0..\.env") do (
      if "%%a"=="DASHSCOPE_API_KEY" set DASHSCOPE_API_KEY=%%b
      if "%%a"=="QWEN_API_KEY" set QWEN_API_KEY=%%b
    )
  ) else (
    echo [Qwen-verify][ERROR] 未找到 API Key
    exit /b 1
  )
)

if "%QWEN_API_KEY%"=="" set "KEY=%DASHSCOPE_API_KEY%"
if "%QWEN_API_KEY%"=="" set "KEY=%QWEN_API_KEY%"

set BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
if not "%QWEN_BASE_URL%"=="" set BASE_URL=%QWEN_BASE_URL%
set MODEL=qwen-plus
if not "%QWEN_MODEL%"=="" set MODEL=%QWEN_MODEL%

REM 脱敏
set "START=%KEY:~0,8%"
set "END=%KEY:~-4%"
echo [Qwen-verify] key=%START%...%END% model=%MODEL%

REM 构造 payload(简化,只测连通性)
echo {"model":"%MODEL%","messages":[{"role":"user","content":"请用 200 字简介《行政立法智能辅助平台》的作用。"}],"max_tokens":512} > %TEMP%\qwen_payload.json

curl -sS -X POST "%BASE_URL%/v1/chat/completions" ^
  -H "Authorization: Bearer %KEY%" ^
  -H "Content-Type: application/json" ^
  -d @%TEMP%\qwen_payload.json > %TEMP%\qwen_resp.json
del %TEMP%\qwen_payload.json

type %TEMP%\qwen_resp.json
echo.
echo [Qwen-verify] 响应已保存到 %TEMP%\qwen_resp.json
del %TEMP%\qwen_resp.json
exit /b 0
