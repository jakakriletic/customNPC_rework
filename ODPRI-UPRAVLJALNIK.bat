@echo off
cd /d "%~dp0"
node overnight\dashboard.mjs launch
if errorlevel 1 pause
