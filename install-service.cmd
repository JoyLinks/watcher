@echo off
chcp 65001
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-service.ps1"