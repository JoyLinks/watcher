<#
.SYNOPSIS
    安装脚本：JOYZL Archive Watcher
.DESCRIPTION
    安装程序到：%ProgramFiles%\joyzl\archive-watcher，
    数据位置为：%ProgramData%\joyzl\archive-watcher，
    创建桌面快捷方式，
    设置开机自动启动，
    调整当前用户为管理员组，
    关闭UAC交互以支持远程更新，
    需要管理员权限执行。
#>

# 请求管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    # 以管理员权限重新启动当前脚本
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    # 退出当前的非管理员进程
    exit
}

# 配置当前用户为管理员并关闭UAC交互
# 桌面模式由当前用户运行，因此需要提权以便于远程更新
Write-Host "正在配置系统权限"

# 获取当前用户
$currentUser = $env:USERNAME
if (-not $currentUser) {
    # 如果环境变量为空，尝试从注册表读取自动登录用户名
    $currentUser = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon" -Name "DefaultUserName" -ErrorAction SilentlyContinue).DefaultUserName
}

# 检查用户是否存在
$localUser = Get-LocalUser -Name $currentUser -ErrorAction SilentlyContinue
if (-not $localUser) {
    Write-Host "用户 $currentUser 无效" -ForegroundColor Red
    exit 1
}

# 检查用户并添加管理员组
$group = "Administrators"
$isMember = Get-LocalGroupMember -Group $group | Where-Object { $_.Name -eq "$env:COMPUTERNAME\$currentUser" }
if (-not $isMember) {
    Write-Host "将用户 $currentUser 加入 $group 组..." -ForegroundColor Yellow
    Add-LocalGroupMember -Group $group -Member $currentUser
    Write-Host "已添加" -ForegroundColor Green
} else {
    Write-Host "用户 $currentUser 已是 $group 组成员" -ForegroundColor Green
}

# 关闭UAC交互
$regPath = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System"
$enableLUA = Get-ItemProperty -Path $regPath -Name "EnableLUA" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty EnableLUA
if ($enableLUA -eq 0) {
    # UAC 已经关闭
} else {
    Set-ItemProperty -Path $regPath -Name "ConsentPromptBehaviorAdmin" -Value 0 -Type DWord -Force
    Set-ItemProperty -Path $regPath -Name "EnableLUA" -Value 0 -Type DWord -Force
    Write-Host "已关闭 UAC 交互，须重启后生效" -ForegroundColor Green
}


# 停止运行实例
Write-Host "停止运行实例"
Stop-Process -Name "watcher" -Force -ErrorAction SilentlyContinue
# 删除旧服务
if (Get-Service -Name "JOYZL-Archive-Watcher" -ErrorAction SilentlyContinue) {
    Write-Host "停止服务"
    .\service.exe stop "JOYZL-Archive-Watcher"
    Write-Host "删除服务"
    .\service.exe delete "JOYZL-Archive-Watcher"
}

Write-Host "安装 JOYZL Archive Watcher"
$ProgramFiles = if ($env:ProgramW6432) { $env:ProgramW6432 } else { $env:ProgramFiles }
$InstallDir = Join-Path $ProgramFiles "joyzl\archive-watcher"
$DataDir = Join-Path $env:ProgramData "joyzl\archive-watcher"

New-Item -ItemType Directory -Force -Path $InstallDir
New-Item -ItemType Directory -Force -Path $DataDir

Write-Host "程序目录: $InstallDir" -ForegroundColor Cyan
Write-Host "数据目录: $DataDir" -ForegroundColor Cyan

Write-Host "复制程序文件"
robocopy $PSScriptRoot $InstallDir /E /COPY:DAT /R:3 /W:10 /NP /NFL /NDL
robocopy $(Join-Path $InstallDir "patterns") $(Join-Path $DataDir "patterns") /E

# 移动配置文件
$PropFile = Join-Path $InstallDir "watcher.properties"
if (Test-Path $PropFile) {
    $DataProp = Join-Path $DataDir "watcher.properties"
    if (Test-Path $DataProp) {
        # 配置文件已存在，保留用户配置"
    } else {
        # 移动配置文件
        Move-Item -Path $PropFile -Destination $DataDir -Force
    }
}

# 设置数据目录权限
icacls $DataDir /grant "Users:(OI)(CI)M" /T

# 创建桌面快捷方式（所有用户）
$ShortcutName = "JOYZL Archive Watcher.lnk"
$ShortcutPath = [Environment]::GetFolderPath("CommonDesktopDirectory")
$ShortcutPath = Join-Path $ShortcutPath $ShortcutName
if (Test-Path $ShortcutPath) { Remove-Item $ShortcutPath -Force }
$WScriptShell = New-Object -ComObject WScript.Shell
$Shortcut = $WScriptShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = Join-Path $InstallDir "watcher.exe"
$Shortcut.WorkingDirectory = $DataDir
$Shortcut.Description = "JOYZL Archive Watcher"
$Shortcut.IconLocation = (Join-Path $InstallDir "watcher.exe") + ",0"
$Shortcut.Save()
Write-Host "已创建桌面快捷方式"

# 设置开机自动启动（所有用户）
$StartupPath = [Environment]::GetFolderPath("CommonStartup")
$StartupPath = Join-Path $StartupPath $ShortcutName
if (Test-Path $StartupPath) { Remove-Item $StartupPath -Force }
$Shortcut = $WScriptShell.CreateShortcut($StartupPath)
$Shortcut.TargetPath = Join-Path $InstallDir "watcher.exe"
$Shortcut.WorkingDirectory = $DataDir
$Shortcut.Description = "JOYZL Archive Watcher"
$Shortcut.IconLocation = (Join-Path $InstallDir "watcher.exe") + ",0"
$Shortcut.Save()
Write-Host "已创建开机启动快捷方式"

# 防火墙规则，允许使用任何端口
$exeFile=Join-Path $InstallDir "watcher.exe"
New-NetFirewallRule -DisplayName "JOYZL Archive Watcher TCP" -Program $exeFile -Direction Inbound -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "JOYZL Archive Watcher UDP" -Program $exeFile -Direction Inbound -Protocol UDP -Action Allow

Write-Host "安装完成" -ForegroundColor Green

if ($enableLUA -eq 0) {
    # UAC 已经关闭，无须重启系统
    Start-Process -FilePath $shortcutPath
    Write-Host "按任意键退出..."
    [void][System.Console]::ReadKey($true)
} else {
    Write-Host "系统将在 10 秒后重启，按任意键取消..." -ForegroundColor Yellow
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    while ($timer.Elapsed.TotalSeconds -lt 10) {
        if ([Console]::KeyAvailable) {
            $key = [Console]::ReadKey($true)
            exit
        }
        Start-Sleep -Milliseconds 500
    }
    Restart-Computer -Force
}