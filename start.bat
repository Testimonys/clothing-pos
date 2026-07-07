@echo off
REM ============================================================
REM 华兴服装店 - POS 系统启动脚本 (Windows)
REM ============================================================
REM 使用方法: 双击运行或在命令行中执行 start.bat
REM ============================================================

set JAR_FILE=clothing-pos-1.0.0.jar
set JAVA_OPTS=-Xms256m -Xmx512m

if not exist "%JAR_FILE%" (
    echo [错误] 未找到 %JAR_FILE%
    echo [提示] 请先执行构建: cd frontend ^&^& npm run build ^&^& cd .. ^&^& mvn clean package -DskipTests
    pause
    exit /b 1
)

echo ========================================
echo   华兴服装店 POS 系统 v1.0.0
echo   启动中...
echo ========================================
echo   JVM 参数: %JAVA_OPTS%
echo   JAR 文件: %JAR_FILE%
echo   访问地址: http://localhost:8080
echo ========================================

java %JAVA_OPTS% -jar "%JAR_FILE%"

pause
