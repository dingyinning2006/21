@echo off
chcp 65001 >nul
echo ============================================================
echo   M5 现实任务模块 - 独立演示 (无需 Spring/MySQL/MongoDB)
echo ============================================================
echo.

REM --- 本机 JDK 路径 ---
set JAVA_HOME=C:\Users\70433\.jdks\openjdk-26.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

REM --- 检查 classes 是否存在 ---
if not exist "target\classes\com\example\demo\agent\contract\m5\M5Demo.class" (
    echo [信息] 首次运行，正在编译...
    call mvnw.cmd compile -q
    if errorlevel 1 (
        echo [错误] 编译失败
        pause
        exit /b 1
    )
)

java -cp target/classes com.example.demo.agent.contract.m5.M5Demo

echo.
pause
