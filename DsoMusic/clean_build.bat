@echo off
chcp 65001 >nul
echo 正在清理构建缓存...

:: 停止 Gradle 守护进程
call gradlew --stop

:: 清理构建目录
call gradlew clean

:: 删除本地构建缓存
if exist "build" (
    rmdir /s /q "build"
)
if exist "app\build" (
    rmdir /s /q "app\build"
)

:: 删除 Gradle 缓存（可选，谨慎使用）
echo 是否删除 Gradle 缓存？(会重新下载依赖，耗时较长)
set /p choice="输入 y 删除，n 跳过: "
if /i "%choice%"=="y" (
    if exist ".gradle" (
        rmdir /s /q ".gradle"
    )
)

echo 清理完成！
pause
