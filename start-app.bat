@echo off
title SmartPark - Full System Launcher
echo ========================================================
echo         SMARTPARK SYSTEM - COMPLETE APPLICATION
echo ========================================================
echo.
echo 1. Launching Spring Boot Server (Port 8081)...
start "SmartPark Backend" cmd /c "cd /d "%~dp0" && .\mvnw.cmd spring-boot:run"
echo.
echo 2. Waiting 6 seconds for server initialization...
timeout /t 6 /nobreak >nul
echo.
echo 3. Opening SmartPark in default web browser...
start http://localhost:8081/
echo.
echo SmartPark is running!
echo Frontend: http://localhost:8081/
echo Backend API: http://localhost:8081/api/slots
echo H2 Console: http://localhost:8081/h2-console
echo.
pause
