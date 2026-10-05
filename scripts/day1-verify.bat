@echo off
REM =============================================================================
REM Day1 启动验证脚本 - Windows 版(等同 day1-verify.sh)
REM
REM 用法: scripts\day1-verify.bat
REM
REM 退出码: 0 = 全过,1 = 有失败
REM =============================================================================

setlocal enabledelayedexpansion

set "BACKEND=%BACKEND%"
if "%BACKEND%"=="" set "BACKEND=http://localhost:8083"
set "API=%BACKEND%/api"
set "PASS=0"
set "FAIL=0"

echo.
echo ============================================================
echo   智立法 · Day1 启动验证
echo   backend: %BACKEND%
echo   time:    %date% %time%
echo ============================================================

call :check "后端 /api/auth/health" ^
  "curl -fsS --max-time 5 "%API%/auth/health" | findstr /C:"\"status\":\"UP\"" >nul"

call :check "GET /api/info/dashboard 返回 code=200" ^
  "curl -fsS --max-time 5 "%API%/info/dashboard" | findstr /C:"\"code\":200" >nul"

call :check "GET /api/info/dashboard/chart 返回 code=200" ^
  "curl -fsS --max-time 5 "%API%/info/dashboard/chart" | findstr /C:"\"code\":200" >nul"

call :check "GET /api/info/map/regulation 返回 code=200" ^
  "curl -fsS --max-time 5 "%API%/info/map/regulation" | findstr /C:"\"code\":200" >nul"

for %%U in (admin leader drafter reviewer evaluator) do (
  call :check "种子账号 %%U / 123456 登录" ^
    "curl -fsS --max-time 5 -H "Content-Type: application/json" -d "{\"username\":\"%%U\",\"password\":\"123456\"}" "%API%/auth/login" | findstr /C:"\"code\":200" >nul"
)

call :check "Knife4j 文档可达" ^
  "curl -fsSI --max-time 5 "%API%/doc.html" 2>nul | findstr /C:"200" >nul"

call :check "Prometheus 抓取端点 /actuator/prometheus" ^
  "curl -fsS --max-time 5 "%BACKEND%/actuator/prometheus" | findstr /C:"jvm_memory_used_bytes" >nul"

echo.
echo ============================================================
echo   验证完成:  ✅ %PASS% 通过   ❌ %FAIL% 失败
echo ============================================================
if %FAIL% gtr 0 exit /b 1
exit /b 0

:check
set "NAME=%~1"
set "CMD=%~2"
echo.
echo ▶ %NAME%
%CMD%
if %ERRORLEVEL%==0 (
  set /a PASS+=1
) else (
  set /a FAIL+=1
  echo   ✗ 失败
)
exit /b 0