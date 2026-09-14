@echo off
cd /d "%~dp0"
title obnova okolja - customNPC_rework
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0obnovi-okolje.ps1"
echo.
echo ===== BAT EXIT %ERRORLEVEL% =====
timeout /t 30
