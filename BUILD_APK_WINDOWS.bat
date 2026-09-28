@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ======================================================
echo   STA POKLONITI RS - AUTOMATSKI BUILD APK-a
echo ======================================================
echo.

if not exist local.properties (
  if defined ANDROID_HOME (
    set "SDK_PATH=%ANDROID_HOME%"
  ) else (
    set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
  )

  if not exist "%SDK_PATH%" (
    echo Android SDK nije pronadjen.
    echo.
    echo Instaliraj Android Studio, pa otvori ovaj projekat bar jednom.
    echo Nakon toga ponovo pokreni BUILD_APK_WINDOWS.bat
    echo.
    pause
    exit /b 1
  )

  powershell -NoProfile -Command "$p=$env:SDK_PATH -replace '\\','/'; Set-Content -Encoding ASCII 'local.properties' ('sdk.dir=' + $p)"
)

call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo BUILD NIJE USPEO. Pogledaj poruku iznad.
  pause
  exit /b 1
)

if not exist OUTPUT mkdir OUTPUT
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "OUTPUT\STA_POKLONITI_RS.apk" >nul

echo.
echo ======================================================
echo GOTOVO!
echo APK je ovde:
echo %CD%\OUTPUT\STA_POKLONITI_RS.apk
echo ======================================================
start "" explorer.exe "%CD%\OUTPUT"
pause
