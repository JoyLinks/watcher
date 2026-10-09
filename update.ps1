<#
.SYNOPSIS
    更新脚本：JOYZL Archive Watcher
.DESCRIPTION
    当前目录：%ProgramData%\joyzl\archive-watcher，
    更新文件：%ProgramData%\joyzl\archive-watcher\stpfiles\joyzl-archive-watcher，
    程序位置：%ProgramFiles%\joyzl\archive-watcher，
    使用方法：随程序更新压缩包一并推送到客户端，远程执行脚本，
    自动提升管理员权限执行。
#>

# 检查管理员权限
$IsAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $IsAdmin) {
    Write-Error "未能获取管理员权限，无法执行更新。"
    exit 1
}

Write-Output "更新 JOYZL Archive Watcher"
$ProgramFiles = if ($env:ProgramW6432) { $env:ProgramW6432 } else { $env:ProgramFiles }
$InstallDir = Join-Path $ProgramFiles "joyzl\archive-watcher"
$DataDir = Join-Path $env:ProgramData "joyzl\archive-watcher"
$SourceDir = ".\stpfiles\joyzl-archive-watcher"

Write-Output "停止运行实例"
$Service = Get-Service -Name "JOYZL-Archive-Watcher" -ErrorAction SilentlyContinue
if($Service){
    Stop-Service -Name "JOYZL-Archive-Watcher" -Force -ErrorAction SilentlyContinue
    Write-Output "服务已停止"
}
$Proc = Get-Process -Name "watcher" -ErrorAction SilentlyContinue
if ($Proc) {
    Stop-Process -Name "watcher" -Force -ErrorAction SilentlyContinue
    Write-Output "运行实例已停止"
}

Write-Output "更新程序文件"
robocopy $SourceDir $InstallDir /E /COPY:DAT /R:3 /W:10 /NP /NFL /NDL
robocopy $(Join-Path $InstallDir "patterns") $(Join-Path $DataDir "patterns") /E

Write-Output "重新启动程序"
if ($Service) {
    try {
        $Service.Start();
        $Service.WaitForStatus('Running', '00:00:12')
        Write-Output "服务已启动"
    } catch {
        Write-Output "启动服务失败: $_"
    }
} else {
    $wshShell = New-Object -ComObject WScript.Shell
    $StartupFolder = $wshShell.SpecialFolders.Item("AllUsersStartup")
    $ShortcutPath = Join-Path $StartupFolder "JOYZL Archive Watcher.lnk"
    if (Test-Path $ShortcutPath) {
        Start-Process -FilePath $ShortcutPath
    } else {
        Start-Process -FilePath (Join-Path $InstallDir "watcher.exe") -WorkingDirectory $DataDir
    }
}

Write-Output "更新完成"
exit 0