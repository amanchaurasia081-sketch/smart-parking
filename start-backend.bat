@echo off
title SmartPark - Spring Boot Backend Server
echo ========================================================
echo         SMARTPARK SYSTEM - BACKEND LAUNCHER
echo ========================================================
echo.
echo Starting Spring Boot Backend on http://localhost:8081 ...
echo Embedded H2 Database: Active
echo H2 Web Console: http://localhost:8081/h2-console
echo.
cd /d "%~dp0"
call .\mvnw.cmd spring-boot:run
pause
