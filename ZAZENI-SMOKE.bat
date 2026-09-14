@echo off
cd /d "%~dp0"
title M0.5 server smoke - customNPC_rework
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0smoke-server.ps1" -AcceptEula
echo.
echo ===== BAT EXIT %ERRORLEVEL% =====
timeout /t 30
