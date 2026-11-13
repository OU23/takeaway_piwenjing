@echo off
chcp 65001 >nul
title 数创星光外卖 · 构建 Android APK
cd /d "%~dp0"

echo ============================================================
echo   数创星光外卖 · Android APK 构建
echo ============================================================
echo.

REM ------------------------------------------------------------------
REM Capacitor 7 的 android 模块要求 sourceCompatibility = 21，
REM 因此 Android 侧必须用 JDK 21；后端 Spring Boot 仍用 JDK 17，两者互不影响。
REM ------------------------------------------------------------------
set "JDK21="
for /d %%i in ("C:\Users\%USERNAME%\.workbuddy\tools\jdk-21*") do set "JDK21=%%~fi"
if not defined JDK21 (
  echo [错误] 未找到 JDK 21（预期位置：C:\Users\%USERNAME%\.workbuddy\tools\jdk-21*）
  echo        安装方式：下载 Temurin 21 后解压到该目录，或设置 JAVA_HOME 指向任意 JDK 21
  pause
  exit /b 1
)
set "JAVA_HOME=%JDK21%"

set "ANDROID_HOME=C:\Users\%USERNAME%\Android\Sdk"
if not exist "%ANDROID_HOME%\platforms" (
  echo [错误] 未找到 Android SDK：%ANDROID_HOME%
  pause
  exit /b 1
)
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

set "MANAGED_NODE=C:\Users\%USERNAME%\.workbuddy\binaries\node\versions\22.22.2-6"
if exist "%MANAGED_NODE%\node.exe" set "PATH=%MANAGED_NODE%;%PATH%"

echo   JDK        %JAVA_HOME%
echo   AndroidSDK %ANDROID_HOME%
echo.

if not exist "node_modules" (
  echo [提示] 首次运行，正在安装前端依赖...
  call npm install --registry=https://registry.npmmirror.com --no-audit --no-fund
)

echo [1/3] 构建 Web 产物（vite build）...
call npm run build
if errorlevel 1 goto :fail

echo [2/3] 同步到 Android 工程（cap sync android）...
call node node_modules\@capacitor\cli\bin\capacitor sync android
if errorlevel 1 goto :fail

echo [3/3] 编译 APK（gradlew assembleDebug）...
pushd android
call gradlew.bat assembleDebug
if errorlevel 1 (popd & goto :fail)
popd

set "APK=%~dp0android\app\build\outputs\apk\debug\app-debug.apk"
echo.
echo ============================================================
if exist "%APK%" (
  echo   构建成功
  echo   产物：%APK%
  for %%f in ("%APK%") do echo   体积：%%~zf 字节
) else (
  echo   构建结束但未找到 APK，请检查上方日志
)
echo ============================================================
pause
exit /b 0

:fail
echo.
echo [失败] 构建中断，请查看上方错误信息。
pause
exit /b 1
