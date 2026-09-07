#!/bin/bash

# 错误退出
set -e

# 检查权限
if [ "$EUID" -ne 0 ]; then
    echo "需要 root 权限来安装到系统目录"
    echo "请使用: sudo $0"
    exit 1
fi

# 创建用户和组
echo "创建用户和组"
if ! getent group "joyzl" > /dev/null; then
	groupadd "joyzl"
fi
if id "joyzl" &>/dev/null; then
	if [ "$(id -gn joyzl)" != "joyzl" ]; then
		usermod -g joyzl joyzl
	fi
else
	useradd -m -s /bin/bash -g joyzl joyzl
fi

# 设置用户密码
echo "joyzl:joyzl.com" | chpasswd

# 设置用户允许无密码提权
if [ -f "/etc/sudoers.d/joyzl" ]; then
	rm -f "/etc/sudoers.d/joyzl"
fi
echo "joyzl ALL=(ALL) NOPASSWD: ALL" > "/etc/sudoers.d/joyzl"
chmod 440 "/etc/sudoers.d/joyzl"

# 将管理用户添加到 joyzl 组
ORIGINAL_USER=${SUDO_USER:-$USER}
if id "$ORIGINAL_USER" &>/dev/null; then
   usermod -a -G joyzl "$ORIGINAL_USER"
fi


# 停止程序
PID=$(pgrep -f joyzl/archive-watcher/bin/watcher 2>/dev/null || true)
if [ -n "$PID" ]; then
	echo "停止当前运行实例: $PID"
	kill $PID
	sleep 6;
fi

echo "开始安装 JOYZL Archive Watcher ..."

# 创建程序目录
mkdir -p /opt/joyzl/archive-watcher
mkdir -p /var/lib/joyzl/archive-watcher
mkdir -p /var/log/joyzl/archive-watcher

# 复制程序文件
cp -rp ./* /opt/joyzl/archive-watcher/
# 复制配置文件
if [ -f "watcher.properties" ]; then
	cp -n watcher.properties /var/lib/joyzl/archive-watcher/
fi

# 移除多余文件
rm -f /opt/joyzl/archive-watcher/install-desktop.sh
rm -f /opt/joyzl/archive-watcher/update.sh

# 设置目录权限
chown -R root:root /opt/joyzl/archive-watcher
chmod -R 755 /opt/joyzl/archive-watcher
chown -R joyzl:joyzl /var/lib/joyzl/archive-watcher
chmod -R 775 /var/lib/joyzl/archive-watcher
chown -R joyzl:joyzl /var/log/archive-watcher
chmod -R 775 /var/log/joyzl/archive-watcher

# 创建桌面入口文件
echo "创建桌面入口"
cat > "/usr/share/applications/joyzl-archive-watcher.desktop" << EOF
[Desktop Entry]
Type=Application
Name=JOYZL Archive Watcher
Comment=JOYZL Archive Watcher Application
Exec=/opt/joyzl/archive-watcher/bin/watcher
Icon=/opt/joyzl/archive-watcher/lib/watcher.png
Path=/var/lib/joyzl/archive-watcher
Terminal=false
StartupNotify=true
Categories="Utility;Development;"
Keywords=archive;watcher;industrial;automation
StartupWMClass=joyzl-archive-watcher
EOF

chmod 644 "/usr/share/applications/joyzl-archive-watcher.desktop"

# 更新桌面数据库
update-desktop-database /usr/share/applications 2>/dev/null || true

echo "安装完成"
