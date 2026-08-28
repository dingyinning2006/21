@echo off
chcp 65001 >nul
echo ============================================================
echo   Spring Boot 应用启动脚本 (适配本机 JDK 26)
echo ============================================================
echo.

REM --- 本机 JDK 路径 ---
set JAVA_HOME=C:\Users\70433\.jdks\openjdk-26.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

echo [信息] Java 版本:
java -version
echo.

REM --- 检查 JAR 是否存在 ---
if not exist "target\demo-0.0.1-SNAPSHOT.jar" (
    echo [错误] 未找到 target\demo-0.0.1-SNAPSHOT.jar
    echo [提示] 请先运行: mvnw.cmd package -DskipTests
    pause
    exit /b 1
)

echo [信息] 正在启动 Spring Boot 应用...
echo [提示] 应用需要 MySQL (localhost:3306/demo) 和 MongoDB 运行
echo [提示] 按 Ctrl+C 停止应用
echo.

java -jar target\demo-0.0.1-SNAPSHOT.jar

pause
