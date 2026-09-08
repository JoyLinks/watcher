#!/bin/bash

# 错误退出
set -e

# 检查是否以 root 权限运行
if [ "$(id -u)" -ne 0 ]; then
	echo "请以 root 权限，或使用 sudo 执行"
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


# 停止服务
sudo systemctl stop joyzl-archive-watcher 2>/dev/null || true
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
cp -rp /opt/joyzl/archive-watcher/patterns /var/lib/joyzl/archive-watcher/patterns
# 复制配置文件（存在则忽略）
cp -n watcher.properties /var/lib/joyzl/archive-watcher/

# 移除多余文件
rm -f /opt/joyzl/archive-watcher/install-service.sh
rm -f /opt/joyzl/archive-watcher/update.sh

# 设置目录权限
chown -R root:root /opt/joyzl/archive-watcher
chmod -R 755 /opt/joyzl/archive-watcher
chown -R joyzl:joyzl /var/lib/joyzl/archive-watcher
chmod -R 775 /var/lib/joyzl/archive-watcher
chown -R joyzl:joyzl /var/log/joyzl/archive-watcher
chmod -R 775 /var/log/joyzl/archive-watcher

# 创建系统服务
cat > /etc/systemd/system/joyzl-archive-watcher.service <<'EOF'
[Unit]
Description=JOYZL Archive Watcher
After=network.target

[Service]
Type=simple
User=joyzl
Group=joyzl
AmbientCapabilities=CAP_NET_BIND_SERVICE
WorkingDirectory=/var/lib/joyzl/archive-watcher

ExecStart=/opt/joyzl/archive-watcher/lib/runtime/bin/java -server -Xms256m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 --module com.joyzl.watcher/com.joyzl.watcher.Application

Restart=on-failure
RestartSec=30s

[Install]
WantedBy=multi-user.target
EOF
systemctl enable joyzl-archive-watcher.service
systemctl daemon-reload

# 移除桌面入口
rm -f /usr/share/applications/joyzl-archive-watcher.desktop
update-desktop-database /usr/share/applications

# 禁用防火墙
echo "正在禁用防火墙..."
if command -v nft &>/dev/null; then
	echo "禁用 nftables"
	# 清空所有规则表
	nft flush ruleset
	# 设置默认策略为接受
	nft add table inet filter
	nft add chain inet filter input { type filter hook input priority 0\; policy accept\; }
	nft add chain inet filter forward { type filter hook forward priority 0\; policy accept\; }
	nft add chain inet filter output { type filter hook output priority 0\; policy accept\; }
	systemctl stop nftables
	systemctl disable nftables
fi
if command -v ufw &>/dev/null; then
	echo "禁用 ufw"
	ufw --force disable
	ufw --force reset
	systemctl stop ufw
	systemctl disable ufw
fi
if command -v iptables &>/dev/null; then
	echo "禁用 iptables"
	# 设置默认策略为 ACCEPT
	iptables -P INPUT ACCEPT
	iptables -P FORWARD ACCEPT
	iptables -P OUTPUT ACCEPT
	# 清空所有规则和自定义链
	iptables -F
	iptables -X
	iptables -t nat -F
	iptables -t nat -X
	iptables -t mangle -F
	iptables -t mangle -X
	iptables -t raw -F
	iptables -t raw -X
	systemctl stop iptables
	systemctl disable iptables
fi
if command -v firewalld &>/dev/null; then
	echo "禁用 firewalld"
	systemctl stop firewalld
	systemctl disable firewalld
fi

echo 启动服务
sudo systemctl start joyzl-archive-watcher

echo "安装完成"
echo "使用以下命令管理服务:"
echo "运行状态: systemctl status joyzl-archive-watcher"
echo "启动服务: systemctl start joyzl-archive-watcher"
echo "停止服务: systemctl stop joyzl-archive-watcher"
