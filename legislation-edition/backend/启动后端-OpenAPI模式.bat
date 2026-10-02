@echo off
chcp 65001 >nul
echo ========================================
echo   智立法 - 启动后端 (openapi profile)
echo ========================================
echo   用途：仅用于生成 OpenAPI 文档
echo   说明：使用 H2 内存数据库，无需 MySQL
echo ========================================
echo.

start "LegislationBackend-OpenAPI" cmd /k "cd /d D:\ProjectSpace\CaseGuardian\legislation_edition\backend && mvn spring-boot:run -DskipTests -Dspring-boot.run.profiles=openapi"

echo 后端启动中（新窗口），请等待 30 秒 ...
echo.
echo 启动完成后可访问：
echo   Knife4j UI     : http://localhost:8083/api/doc.html
echo   Swagger UI     : http://localhost:8083/api/swagger-ui.html
echo   OpenAPI JSON   : http://localhost:8083/api/v3/api-docs
echo.
pause