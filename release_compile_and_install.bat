@echo off
setlocal

set "PROJECT_DIR=D:\Engineering\Android\Recollect"
set "APK=%PROJECT_DIR%\app\build\outputs\apk\release\app-release.apk"
set "SERIAL="

if not exist "%PROJECT_DIR%\gradlew.bat" (
    echo [ERROR] Project directory or gradlew.bat not found: %PROJECT_DIR%
    exit /b 1
)

cd /d "%PROJECT_DIR%"

echo [1/3] Checking connected devices...
adb devices
for /f "skip=1 tokens=1,2" %%a in ('adb devices') do (
    if "%%b"=="device" if not defined SERIAL set "SERIAL=%%a"
)
if not defined SERIAL (
    echo [ERROR] No authorized Android device found. Run: adb devices
    exit /b 1
)
echo Using device: %SERIAL%

echo [2/3] Building Release APK (R8 minify + resource shrink)...
call "%PROJECT_DIR%\gradlew.bat" :app:assembleRelease
if errorlevel 1 (
    echo [ERROR] Gradle build failed.
    exit /b 1
)

if not exist "%APK%" (
    echo [ERROR] APK not found: %APK%
    exit /b 1
)

echo [3/3] Installing APK...
adb -s "%SERIAL%" install -r "%APK%"
if errorlevel 1 (
    echo [ERROR] adb install failed.
    echo [HINT] INSTALL_FAILED_UPDATE_INCOMPATIBLE means the installed build has a different signature.
    echo        Export data inside the app first, then run: adb uninstall org.recollect
    exit /b 1
)

echo.
echo Done. APK: %APK%
for %%F in ("%APK%") do echo Size: %%~zF bytes
echo Device: %SERIAL%
exit /b 0
