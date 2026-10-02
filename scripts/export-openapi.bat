@echo off
REM =============================================================================
REM 智立法 - Windows 一键导出 OpenAPI 3 JSON 文档
REM
REM 用法:
REM   scripts\export-openapi.bat
REM   set OUTPUT_PATH=D:\docs\openapi.json ^&^& scripts\export-openapi.bat
REM =============================================================================
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set BACKEND_DIR=%SCRIPT_DIR%legislation-edition\backend
set OUTPUT_PATH=%OUTPUT_PATH:~1%
if "%OUTPUT_PATH%"=="" set OUTPUT_PATH=%SCRIPT_DIR%openapi.json
set DOCS_BASE=%DOCS_BASE%
if "%DOCS_BASE%"=="" set DOCS_BASE=http://127.0.0.1:8083

where mvn >nul 2>nul
if errorlevel 1 (
  echo ERROR: mvn 未安装或不在 PATH
  exit /b 1
)

cd /d "%BACKEND_DIR%"
echo [export-openapi] 构建 jar ...
call mvn -q -DskipTests package
if errorlevel 1 exit /b 1

if not exist target\legislation-edition-backend.jar (
  echo ERROR: jar not built
  exit /b 1
)

echo [export-openapi] 启动后端 (openapi profile) ...
set SPRING_PROFILES_ACTIVE=openapi
start /b "" java -jar target\legislation-edition-backend.jar --server.port=8083 --server.servlet.context-path=/api > %TEMP%\openapi-export.log 2>&1

REM 轮询等待
set /a ready=0
for /l %%i in (1,1,60) do (
  curl -sf "%DOCS_BASE%/actuator/health" >nul 2>&1
  if not errorlevel 1 (
    set /a ready=1
    echo [export-openapi] backend ready
    goto :ready
  )
  timeout /t 1 /nobreak >nul
)

:ready
if %ready%==0 (
  echo ERROR: backend 未就绪
  taskkill /f /im java.exe /fi "WINDOWTITLE eq java -jar*" >nul 2>&1
  exit /b 1
)

REM 导出
mkdir "%OUTPUT_PATH:~0,-12%" 2>nul
curl -sf "%DOCS_BASE%/v3/api-docs" -o "%OUTPUT_PATH%"
echo [export-openapi] saved to %OUTPUT_PATH%

REM 关掉后端
taskkill /f /im java.exe /fi "WINDOWTITLE eq java -jar*" >nul 2>&1

echo [export-openapi] done