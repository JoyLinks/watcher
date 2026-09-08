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
	xdg-open /usr/share/applications/joyzl-archive-watcher.desktop
else
	echo 启动服务
	sudo systemctl start joyzl-archive-watcher
fi

echo "更新完成"
