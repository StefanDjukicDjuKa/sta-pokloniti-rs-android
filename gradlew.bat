@echo off
setlocal EnableExtensions
set GRADLE_VERSION=8.9
set GRADLE_HOME_DIR=%USERPROFILE%\.gradle\sta-pokloniti-dist\gradle-%GRADLE_VERSION%
set GRADLE_ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_HOME_DIR%\bin\gradle.bat" (
  echo [STA POKLONITI RS] Prvo pokretanje - preuzimam Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
  if errorlevel 1 exit /b 1

  if exist "%USERPROFILE%\.gradle\sta-pokloniti-dist" rmdir /s /q "%USERPROFILE%\.gradle\sta-pokloniti-dist"
  mkdir "%USERPROFILE%\.gradle\sta-pokloniti-dist" >nul 2>nul
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%USERPROFILE%\.gradle\sta-pokloniti-dist' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME_DIR%\bin\gradle.bat" %*
exit /b %errorlevel%
