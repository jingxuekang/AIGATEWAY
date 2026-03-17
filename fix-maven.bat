@echo off
echo 修复 Maven 配置...
echo.

REM 1. 清理失败的缓存
echo [1/3] 清理 Maven 缓存...
if exist "%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-parent\3.2.5" (
    rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-parent\3.2.5"
    echo 已清理 Spring Boot 缓存
)

REM 2. 备份旧配置
echo.
echo [2/3] 备份旧配置...
if exist "%USERPROFILE%\.m2\settings.xml" (
    copy "%USERPROFILE%\.m2\settings.xml" "%USERPROFILE%\.m2\settings.xml.backup"
    echo 已备份到 settings.xml.backup
)

REM 3. 复制新配置
echo.
echo [3/3] 更新配置文件...
copy /y maven-settings.xml "%USERPROFILE%\.m2\settings.xml"
echo 已更新 Maven 配置

echo.
echo ========================================
echo 修复完成！
echo ========================================
echo.
echo 现在请在 IDEA 中执行以下操作：
echo 1. 右键点击项目根目录
echo 2. 选择 Maven -^> Reload Project
echo 3. 等待依赖重新下载
echo 4. 重新启动 AdminApplication
echo.
pause
