<#
.SYNOPSIS
    安装 JOYZL Archive Watcher 为 Windows 服务
.DESCRIPTION
    安装程序到：%ProgramFiles%\joyzl\archive-watcher，
    数据位置为：%ProgramData%\joyzl\archive-watcher，
    注册为 Windows 服务，
    需要管理员权限执行。
#>

# PowerShell 2.0 兼容：取脚本自身路径与所在目录
$ScriptFile = $MyInvocation.MyCommand.Path
$ScriptPath = Split-Path -Parent $ScriptFile

# 请求管理员权限
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$ScriptFile`"" -Verb RunAs
    exit
}

Write-Host "停止运行实例"
Stop-Process -Name "watcher" -Force -ErrorAction SilentlyContinue

# 删除旧服务
$ServiceExe = Join-Path $ScriptPath 'service.exe'
if (Get-Service -Name "JOYZL-Archive-Watcher" -ErrorAction SilentlyContinue) {
    Write-Host "停止服务"
    & $ServiceExe stop "JOYZL-Archive-Watcher"
    Write-Host "删除服务"
    & $ServiceExe delete "JOYZL-Archive-Watcher"
}

Write-Host "删除快捷方式"
$ShortcutName = "JOYZL Archive Watcher.lnk"
$wshShell = New-Object -ComObject WScript.Shell
$ShortcutPath = Join-Path $wshShell.SpecialFolders.Item("AllUsersDesktop") $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue
$ShortcutPath = Join-Path $wshShell.SpecialFolders.Item("AllUsersStartup") $ShortcutName
Remove-Item -Path $ShortcutPath -Recurse -Force -ErrorAction SilentlyContinue

# 边缘端以本地系统(LocalSystem)运行
# 已具有足够权限由远程发起程序更新
Write-Host "安装 JOYZL Archive Watcher"
$ProgramFiles = if ($env:ProgramW6432) { $env:ProgramW6432 } else { $env:ProgramFiles }
$InstallDir = Join-Path $ProgramFiles "joyzl\archive-watcher"
$DataDir = Join-Path $env:ProgramData "joyzl\archive-watcher"

Write-Host "创建目录"
New-Item -ItemType Directory -Force -Path $InstallDir
New-Item -ItemType Directory -Force -Path $DataDir
Write-Host "程序目录: $InstallDir" -ForegroundColor Cyan
Write-Host "数据目录: $DataDir" -ForegroundColor Cyan

Write-Host "复制程序文件"
robocopy $ScriptPath $InstallDir /E /COPY:DAT /R:3 /W:10 /NP /NFL /NDL
robocopy $(Join-Path $InstallDir "patterns") $(Join-Path $DataDir "patterns") /E

Write-Host "移动配置文件"
$PropFile = Join-Path $InstallDir "watcher.properties"
if (Test-Path $PropFile) {
    $DataProp = Join-Path $DataDir "watcher.properties"
    if (Test-Path $DataProp) {
        Write-Host "配置文件已存在，保留用户配置"
    } else {
        Move-Item -Path $PropFile -Destination $DataDir -Force
		Write-Host "已移动配置文件"
    }
}

Write-Host "设置数据目录权限"
& icacls $DataDir /grant 'NT AUTHORITY\SYSTEM:M' /T
& icacls $DataDir /grant 'Users:(OI)(CI)M' /T

Write-Host "注册服务"
$ServiceExe = Join-Path $InstallDir "service.exe"
$serviceArgs = @(
    "//IS//JOYZL-Archive-Watcher",
    "--DisplayName=`"JOYZL Archive 设备文件自动归集服务`"",
    "--Description=`"JOYZL Archive 设备文件自动归集服务，用于设备实输出文件自动归集到服务器。如果停止该服务，则被监控的设备输出的文件不会被上传到服务器。`"",
    "--JavaHome=`"$InstallDir\runtime`"",
    "--ServiceUser=LocalSystem",
    "--Startup=auto",
    "--StartMode=jvm",
    "--StartPath=`"$DataDir`"",
    "--StartMethod=start",
    "--StartClass=com.joyzl.watcher.Application",
    "++JvmOptions=-Xms128m",
    "++JvmOptions=-Xmx512m",
    "++JvmOptions=-Dfile.encoding=UTF-8",
    "++JvmOptions=-Duser.timezone=GMT+08",
    "++JvmOptions=-Duser.dir=`"$DataDir`"",
    "--StopMode=jvm",
    "--StopPath=`"$DataDir`"",
    "--StopClass=com.joyzl.watcher.Application",
    "--StopMethod=stop",
    "--StopTimeout=30",
    "--StdOutput=`"$DataDir\log\out.log`"",
    "--StdError=`"$DataDir\log\err.log`"",
    "--LogPath=`"$DataDir\log`"",
    "--LogPrefix=daemon",
    "--PidFile=pid"
)

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $ServiceExe
$psi.Arguments = ($serviceArgs -join ' ')    # ← 数组 → 字符串
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)
$proc.WaitForExit()
$code = $proc.ExitCode
if ($code -ne 0) {
    Write-Host "服务注册失败: $code" -ForegroundColor Red
} else {
    Write-Host "服务注册成功" -ForegroundColor Green
}

Write-Host "设置防火墙规则"
# 防火墙规则，允许使用任何端口（PowerShell 2.0 兼容：改用 netsh）
netsh advfirewall firewall add rule name="JOYZL Archive Watcher TCP" dir=in action=allow program="$ServiceExe" protocol=TCP
netsh advfirewall firewall add rule name="JOYZL Archive Watcher UDP" dir=in action=allow program="$ServiceExe" protocol=UDP

Write-Host "启动服务"
try {
    Start-Service -Name "JOYZL-Archive-Watcher" -ErrorAction Stop
    $waited = 0
    while ((Get-Service "JOYZL-Archive-Watcher").Status -ne 'Running' -and $waited -lt 10) {
        Start-Sleep -Seconds 1
        $waited++
    }
    if ((Get-Service "JOYZL-Archive-Watcher").Status -eq 'Running') {
        Write-Host "服务已启动"
    } else {
        Write-Host "服务启动失败，当前状态: $((Get-Service "JOYZL-Archive-Watcher").Status)"
    }
} catch {
    Write-Host "启动服务失败: $_"
}

Write-Host "安装完成" -ForegroundColor Green
Write-Host "按任意键退出..."
[void][System.Console]::ReadKey($true)