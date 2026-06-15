<#
.SYNOPSIS
    在“所有用户”的“启动”文件夹创建快捷方式。
.DESCRIPTION
    脚本会自动查找与脚本同目录下的 watcher.exe 程序，为其创建快捷方式并放置到
    公共启动文件夹（C:\ProgramData\...\StartUp）。需要管理员权限运行。
    兼容 Windows 7 (PowerShell 2.0) 及以上系统。
.EXAMPLE
    .\AddWatcherToStartup.ps1
    默认覆盖方式添加 watcher.exe 到启动项。
.EXAMPLE
    .\AddWatcherToStartup.ps1 -Arguments "-service"
    附带启动参数并添加。
.NOTES
    必须以管理员身份运行。快捷方式名称固定为“watcher.lnk”。
#>

param(
    [string]$Arguments = "",
    [string]$WorkingDirectory = "",
    [switch]$NoClobber
)

# 获取脚本所在目录（兼容 PowerShell 2.0）
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetExe = Join-Path $scriptDir "watcher.exe"
$shortcutName = "watcher.lnk"

# 检查当前是否具有管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    Start-Sleep -Milliseconds 500
    # 关键命令：以管理员权限重新启动当前脚本
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    # 退出当前的非管理员进程
    exit
}

# 获取公共启动文件夹路径
$commonStartup = [Environment]::GetFolderPath("CommonStartup")
if (-not $commonStartup) {
    $commonStartup = "C:\ProgramData\Microsoft\Windows\Start Menu\Programs\StartUp"
}
if (-not (Test-Path $commonStartup)) {
    Write-Host "错误: 公共启动文件夹不存在: $commonStartup" -ForegroundColor Red
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# 检查程序是否存在
if (-not (Test-Path $targetExe)) {
    Write-Host "错误: 在当前目录未找到 JOYZL Watcher 程序 watcher.exe: $targetExe" -ForegroundColor Red
    Write-Host "请确保 watcher.exe 和本脚本放在同一个文件夹中。" -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# 设置工作目录
if ([string]::IsNullOrEmpty($WorkingDirectory)) {
    $WorkingDirectory = $scriptDir
}

# 创建快捷方式（使用 COM 对象，兼容 PowerShell 2.0）
$shortcutName = Join-Path $commonStartup $shortcutName
try {
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($shortcutName)
    $shortcut.TargetPath = $targetExe
    $shortcut.Arguments = $Arguments
    $shortcut.WorkingDirectory = $WorkingDirectory
    $shortcut.Save()

    Write-Host "已在所有用户的启动文件夹中创建快捷方式；" -ForegroundColor Green
    Write-Host "该程序将在下次任何用户登录时自动启动。" -ForegroundColor Yellow
}
catch {
    Write-Host "创建快捷方式时发生错误: $($_.Exception.Message)" -ForegroundColor Red
}
finally {
    Write-Host "按任意键退出..." -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}