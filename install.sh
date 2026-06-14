#!/bin/bash

set -e
echo "开始安装 JOYZL SCADA Watcher ..."

# 检查权限
if [ "$EUID" -ne 0 ]; then
    echo "需要 root 权限来安装到系统目录"
    echo "请使用: sudo $0"
    exit 1
fi

# 创建程序目录
mkdir -p /opt/joyzl/archive-watcher
# 复制程序文件
cp -rp ./* /opt/joyzl/archive-watcher/
# 设置目录权限
chmod -R 755 /opt/joyzl/archive-watcher

# 创建桌面入口文件
cat > "/usr/share/applications/joyzl-archive-watcher.desktop" << EOF
[Desktop Entry]
Type=Application
Name=JOYZL Archive Watcher
Comment=JOYZL Archive Watcher Application
Exec=/opt/joyzl/archive-watcher/bin/watcher
Icon=/opt/joyzl/archive-watcher/lib/watcher.png
Path=/opt/joyzl/archive-watcher
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
