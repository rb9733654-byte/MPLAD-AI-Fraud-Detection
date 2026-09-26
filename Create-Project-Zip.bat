@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Create-Project-Zip.ps1"
if errorlevel 1 (
  echo.
  echo ZIP creation failed. Read the message above.
  pause
)
