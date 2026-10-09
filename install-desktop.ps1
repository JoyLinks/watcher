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

# PowerShell 2.0 兼容：取脚本自身路径与所在目录
$ScriptFile = $MyInvocation.MyCommand.Path
$ScriptPath = Split-Path -Parent $ScriptFile

# 请求管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "正在请求管理员权限..." -ForegroundColor Yellow
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$ScriptFile`"" -Verb RunAs
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

# 检查用户是否存在（PowerShell 2.0 兼容：改用 net user）
net user $currentUser 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "用户 $currentUser 无效" -ForegroundColor Red
    exit 1
}

# 检查用户并添加管理员组（PowerShell 2.0 兼容：改用 net localgroup）
$group = "Administrators"
$groupMembers = net localgroup $group 2>&1
$isMember = $false
foreach ($m in $groupMembers) {
    $mText = "$m".Trim()
    if ($mText -eq $currentUser -or $mText -eq "$env:COMPUTERNAME\$currentUser") {
        $isMember = $true
        break
    }
}
if (-not $isMember) {
    Write-Host "将用户 $currentUser 加入 $group 组..." -ForegroundColor Yellow
    net localgroup $group $currentUser /add
    Write-Host "已添加" -ForegroundColor Green
} else {
    Write-Host "用户 $currentUser 已是 $group 组成员" -ForegroundColor Green
}

# 关闭UAC交互
$regPath = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System"
Set-ItemProperty -Path $regPath -Name "ConsentPromptBehaviorAdmin" -Value 0 -Type DWord -Force
Set-ItemProperty -Path $regPath -Name "EnableLUA" -Value 0 -Type DWord -Force
Write-Host "已关闭 UAC 交互，须重启后生效" -ForegroundColor Green

# 停止运行实例
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
icacls $DataDir /grant "Users:(OI)(CI)M" /T

Write-Host "创建快捷方式"
# 创建桌面快捷方式（所有用户）
$ShortcutName = "JOYZL Archive Watcher.lnk"
$WScriptShell = New-Object -ComObject WScript.Shell
$ShortcutPath = Join-Path $WScriptShell.SpecialFolders.Item("AllUsersDesktop") $ShortcutName
if (Test-Path $ShortcutPath) { Remove-Item $ShortcutPath -Force }
$Shortcut = $WScriptShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = Join-Path $InstallDir "watcher.exe"
$Shortcut.WorkingDirectory = $DataDir
$Shortcut.Description = "JOYZL Archive Watcher"
$Shortcut.IconLocation = (Join-Path $InstallDir "watcher.exe") + ",0"
$Shortcut.Save()
Write-Host "已创建桌面快捷方式"

# 设置开机自动启动（所有用户）
$StartupPath = Join-Path $WScriptShell.SpecialFolders.Item("AllUsersStartup") $ShortcutName
if (Test-Path $StartupPath) { Remove-Item $StartupPath -Force }
$Shortcut = $WScriptShell.CreateShortcut($StartupPath)
$Shortcut.TargetPath = Join-Path $InstallDir "watcher.exe"
$Shortcut.WorkingDirectory = $DataDir
$Shortcut.Description = "JOYZL Archive Watcher"
$Shortcut.IconLocation = (Join-Path $InstallDir "watcher.exe") + ",0"
$Shortcut.Save()
Write-Host "已创建开机启动快捷方式"

Write-Host "设置防火墙规则"
# 防火墙规则，允许使用任何端口（PowerShell 2.0 兼容：改用 netsh）
$exeFile = Join-Path $InstallDir "watcher.exe"
netsh advfirewall firewall add rule name="JOYZL Archive Watcher TCP" dir=in action=allow program="$exeFile" protocol=TCP
netsh advfirewall firewall add rule name="JOYZL Archive Watcher UDP" dir=in action=allow program="$exeFile" protocol=UDP

Start-Process -FilePath $ShortcutPath
Write-Host "安装完成" -ForegroundColor Green
Write-Host "按任意键退出..."
[void][System.Console]::ReadKey($true)
