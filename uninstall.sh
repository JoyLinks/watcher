#!/bin/bash

# 检查是否以 root 权限运行
if [ "$(id -u)" -ne 0 ]; then
    echo "请以 root 权限，或使用 sudo 执行"
    exit 1
fi

# 停止服务
systemctl stop joyzl-archive-watcher
# 停止程序
PID=$(pgrep -f joyzl/archive-watcher/bin/watcher 2>/dev/null || true)
if [ -n "$PID" ]; then
	echo "停止当前运行实例: $PID"
	kill $PID
	sleep 6;
fi

# 删除服务
systemctl disable joyzl-archive-watcher.service
rm -f /etc/systemd/system/joyzl-archive-watcher.service
systemctl daemon-reload

# 删除目录
rm -r /opt/joyzl/archive-watcher
rm -r /var/log/joyzl/archive-watcher
rm -r /var/lib/joyzl/archive-watcher

# 删除入口
rm -f /usr/share/applications/joyzl-archive-watcher.desktop
update-desktop-database /usr/share/applications

# 删除用户和组
# userdel joyzl
# groupdel joyzl

echo "卸载完成"