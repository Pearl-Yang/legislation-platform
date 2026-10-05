@echo off
REM =============================================================================
REM Day2 端到端冒烟脚本 - Windows 版
REM 提示:Windows 下的 token 解析比较啰嗦,推荐在 Git Bash 或 WSL 中跑:
REM       bash scripts/day2-smoke.sh
REM
REM 本脚本只做健康检查 + 接口契约验证
REM =============================================================================

setlocal enabledelayedexpansion

set "BACKEND=%BACKEND%"
if "%BACKEND%"=="" set "BACKEND=http://localhost:8083"
set "API=%BACKEND%/api"
set "PASS=0"
set "FAIL=0"

echo ============================================================
echo   智立法 · Day2 健康检查 ^(Windows 版^)
echo   backend: %BACKEND%
echo ============================================================

call :check "后端 /api/auth/health" ^
  "curl -fsS --max-time 5 "%API%/auth/health" | findstr /C:"\"status\":\"UP\"" >nul"

call :check "GET /api/legislative-project/list" ^
  "curl -fsS --max-time 5 "%API%/legislative-project/list" | findstr /C:"\"code\":" >nul"

call :check "GET /api/draft/list?projectId=1" ^
  "curl -fsS --max-time 5 "%API%/draft/list?projectId=1" | findstr /C:"\"code\":" >nul"

call :check "GET /api/review/rule-list" ^
  "curl -fsS --max-time 5 "%API%/review/rule-list" | findstr /C:"\"code\":" >nul"

echo.
echo ============================================================
echo   完成:  [V] %PASS% 通过   [X] %FAIL% 失败
echo ============================================================
echo 提示: 完整端到端冒烟需登录态,请用 bash 跑:
echo   bash scripts/day2-smoke.sh
echo ============================================================
if %FAIL% gtr 1 exit /b 1
exit /b 0

:check
set "NAME=%~1"
set "CMD=%~2"
echo.
echo ▶ %NAME%
%CMD%
if errorlevel 1 (
  echo   [X] 失败
  set /a FAIL+=1
) else (
  set /a PASS+=1
)
exit /b 0