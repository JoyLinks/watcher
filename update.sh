#!/bin/bash

# 错误退出
set -e

# 检查权限按需切换用户
if ! sudo -n true 2>/dev/null; then
    if [ "$USER" != "joyzl" ] && sudo -n -u joyzl true 2>/dev/null; then
        exec sudo -u joyzl "$0" "$@"
    else
        echo "未能通过当前用户或 joyzl 用户获取管理权限"
        exit 1
    fi
fi

# 服务更新必须脱离服务进程(Systemd)
# 否则 systemctl stop joyzl-archive-watcher 执行后，关联进程全部终止（包括当前脚本）
if grep -q "joyzl-archive-watcher" /proc/self/cgroup 2>/dev/null; then
	ORIGINAL_WORKDIR=$(pwd)
	SCRIPT_ABS=$(realpath "$0")
    sudo systemd-run --slice=system.slice \
        --unit=update-$(date +%s) \
        --working-directory="$ORIGINAL_WORKDIR" \
        bash "$SCRIPT_ABS"
    exit 0
fi
exec >> script.log 2>&1

echo "停止当前实例"
sudo systemctl stop joyzl-archive-watcher 2>/dev/null || true
sudo pkill -f "/opt/joyzl/archive-watcher/bin/watcher" 2>/dev/null || true
sleep 3
sudo pkill -9 -f "/opt/joyzl/archive-watcher/bin/watcher" 2>/dev/null || true
sleep 3

# 程序所在目录
# /opt/joyzl/archive-watcher
# 当前所在目录
# /var/lib/joyzl/archive-watcher
# 更新文件所在目录
# /var/lib/joyzl/archive-watcher/stpfiles
# 更新压缩包必须存在 joyzl-archive-watcher 目录
# 压缩包解压后位于 stpfiles 目录中

echo "开始更新 JOYZL Archive Watcher ..."
sudo cp -rp ./stpfiles/joyzl-archive-watcher/. /opt/joyzl/archive-watcher/
sudo cp -rp ./stpfiles/joyzl-archive-watcher/patterns/. /var/lib/joyzl/archive-watcher/patterns

# 移除多余文件
sudo rm -f /opt/joyzl/archive-watcher/install.sh
sudo rm -f /opt/joyzl/archive-watcher/update.sh
sudo rm -f /opt/joyzl/archive-watcher/kiosk.sh

# 设置目录权限
sudo chmod -R 755 /opt/joyzl/archive-watcher
sudo chmod -R 755 /var/lib/joyzl/archive-watcher

# 启动程序
if [ -f "/usr/share/applications/joyzl-archive-watcher.desktop" ]; then
	echo "启动程序"
	sudo -u $USER -i sh -c "cd /var/lib/joyzl/archive-watcher && DISPLAY=:0 /opt/joyzl/archive-watcher/bin/watcher &"
else
	echo "启动服务"
	sudo systemctl start joyzl-archive-watcher
fi

echo "更新完成"
