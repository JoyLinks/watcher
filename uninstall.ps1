<#
.SYNOPSIS
    卸载 JOYZL Archive Watcher
.DESCRIPTION
    停止并删除 Windows 服务，
    删除程序文件 Program Files\joyzl\archive-watcher，
    删除数据文件 ProgramData\joyzl\archive-watcher，
    需要管理员权限运行。
#>

# 请求管理员权限
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

Write-Host "卸载 JOYZL Archive Watcher"

Write-Host "停止运行实例"
Stop-Process -Name "watcher" -Force -ErrorAction SilentlyContinue

# 停止并删除服务
if (Get-Service -Name "JOYZL-Archive-Watcher" -ErrorAction SilentlyContinue) {
    Write-Host "停止服务"
    .\service.exe stop "JOYZL-Archive-Watcher"
    Write-Host "删除服务"
    .\service.exe delete "JOYZL-Archive-Watcher"
}

# 删除程序文件
$InstallDir = Join-Path $env:ProgramFiles "joyzl\archive-watcher"
Remove-Item -Path $InstallDir -Recurse -Force -ErrorAction SilentlyContinue

# 删除数据文件
$DataDir = Join-Path $env:ProgramData "joyzl\archive-watcher"
Remove-Item -Path $DataDir -Recurse -Force -ErrorAction SilentlyContinue

# 删除防火墙规则
Remove-NetFirewallRule -DisplayName "JOYZL Archive Watcher TCP" -ErrorAction SilentlyContinue
Remove-NetFirewallRule -DisplayName "JOYZL Archive Watcher UDP" -ErrorAction SilentlyContinue

# 删除快捷方式
$ShortcutName = "JOYZL Archive Watcher.lnk"
$ShortcutPath = [Environment]::GetFolderPath("CommonDesktopDirectory")
$ShortcutPath = Join-Path $ShortcutPath $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue
$ShortcutPath = [Environment]::GetFolderPath("CommonStartup")
$ShortcutPath = Join-Path $ShortcutPath $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "卸载完成" -ForegroundColor Green
Write-Host "按任意键退出..."
[void][System.Console]::ReadKey($true)
