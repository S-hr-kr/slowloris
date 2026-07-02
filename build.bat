@echo off
setlocal
echo ============================================================
echo  Slowloris Backend - Maven Build (Windows)
echo ============================================================

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] mvn not found. Please install Maven and add it to PATH.
    exit /b 1
)

cd /d "%~dp0"

echo [1/1] Building all modules (skip tests)...
mvn clean package -DskipTests

if errorlevel 1 (
    echo [ERROR] Build failed.
    exit /b 1
)

echo.
echo [OK] Build succeeded. JARs are ready under each service's target/ directory.
echo      Next: run deploy.sh on the server, or copy files to the server and run docker compose up.
endlocal
