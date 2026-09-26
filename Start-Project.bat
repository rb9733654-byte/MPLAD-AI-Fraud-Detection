@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-Project.ps1"
if errorlevel 1 (
  echo.
  echo Project startup stopped. Read the message above, fix the prerequisite, then run this file again.
  pause
)
