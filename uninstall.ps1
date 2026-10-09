<#
.SYNOPSIS
    卸载 JOYZL Archive Watcher
.DESCRIPTION
    停止并删除 Windows 服务，
    删除程序文件 Program Files\joyzl\archive-watcher，
    删除数据文件 ProgramData\joyzl\archive-watcher，
    需要管理员权限运行。
#>

# PowerShell 2.0 兼容：取脚本自身路径
$ScriptFile = $MyInvocation.MyCommand.Path
$ScriptPath = Split-Path -Parent $ScriptFile

# 请求管理员权限
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$ScriptFile`"" -Verb RunAs
    exit
}

Write-Host "卸载 JOYZL Archive Watcher"

Write-Host "停止运行实例"
Stop-Process -Name "watcher" -Force -ErrorAction SilentlyContinue

# 停止并删除服务
$ServiceExe = Join-Path $ScriptPath 'service.exe'
if (Get-Service -Name "JOYZL-Archive-Watcher" -ErrorAction SilentlyContinue) {
    Write-Host "停止服务"
    & $serviceExe stop "JOYZL-Archive-Watcher"
    Write-Host "删除服务"
    & $serviceExe delete "JOYZL-Archive-Watcher"
}

Write-Host "删除程序文件"
$InstallDir = Join-Path $env:ProgramFiles "joyzl\archive-watcher"
Remove-Item -Path $InstallDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "删除数据文件"
$DataDir = Join-Path $env:ProgramData "joyzl\archive-watcher"
Remove-Item -Path $DataDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "删除防火墙规则"
# 删除防火墙规则（PowerShell 2.0 兼容：改用 netsh）
netsh advfirewall firewall delete rule name="JOYZL Archive Watcher TCP" | Out-Null
netsh advfirewall firewall delete rule name="JOYZL Archive Watcher UDP" | Out-Null

Write-Host "删除快捷方式"
$ShortcutName = "JOYZL Archive Watcher.lnk"
$wshShell = New-Object -ComObject WScript.Shell
$ShortcutPath = Join-Path $wshShell.SpecialFolders.Item("AllUsersDesktop") $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue
$ShortcutPath = Join-Path $wshShell.SpecialFolders.Item("AllUsersStartup") $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "卸载完成" -ForegroundColor Green
Write-Host "按任意键退出..."
[void][System.Console]::ReadKey($true)