#!/bin/sh

java --version
echo

mvn --version
echo

# 移除目标目录
rm -rf publish

# 编译
mvn -f pom.xml clean package -U

VERSION=1.2.0
ARCH=$(arch)

echo Build executable JOYZL Archive Watcher
# https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html

jpackage \
	--name watcher\
	--type app-image\
	--app-version $VERSION\
	--vendor www.joyzl.com\
	--copyright www.joyzl.com\
	--description "JOYZL Archive Watcher"\
	--icon publish/watcher/watcher.png\
	--dest publish/joyzl-archive-watcher\
	--module-path publish/watcher/lib\
	--module com.joyzl.watcher/com.joyzl.watcher.Application\
	--add-modules jdk.charsets\
	--add-modules jdk.localedata\
	--jlink-options --compress=2\
	--jlink-options --no-header-files\
	--jlink-options --no-man-pages\
	--jlink-options --bind-services\
	--jlink-options --include-locales=zh-cn\
	--java-options -Xms256m\
	--java-options -Xmx1024m\
	--java-options -Dfile.encoding=UTF-8\
	--java-options -Duser.timezone=GMT+08\
	--verbose

# publish/joyzl-archive-watcher/watcher -> publish/joyzl-archive-watcher
mv publish/joyzl-archive-watcher/watcher/* publish/joyzl-archive-watcher/
rmdir publish/joyzl-archive-watcher/watcher

mkdir publish/joyzl-archive-watcher/patterns
cp publish/watcher/*.xml publish/joyzl-archive-watcher/patterns/
cp publish/watcher/watcher.properties publish/joyzl-archive-watcher/watcher.properties
cp publish/watcher/install-desktop.sh publish/joyzl-archive-watcher/install-desktop.sh
cp publish/watcher/install-service.sh publish/joyzl-archive-watcher/install-service.sh
cp publish/watcher/uninstall.sh publish/joyzl-archive-watcher/uninstall.sh
cp publish/watcher/update.sh publish/joyzl-archive-watcher/update.sh
cp publish/watcher/readme.md publish/joyzl-archive-watcher/readme.md

# 可执行文件
chmod +x publish/joyzl-archive-watcher/*.sh

# 创建压缩包
tar -czf "publish/joyzl-archive-watcher_linux-${ARCH}_${VERSION}.tar.gz" -C publish joyzl-archive-watcher


# 创建安装包
# cd publish
# jpackage \
#	--about-url http://www.joyzl.com\
#	--app-image joyzl-watcher\
#	--install-dir /opt/joyzl/watcher\
#	--resource-dir joyzl-watcher\
#	--linux-shortcut

# deb Ubuntu, Debian
# rpm RHEL, CentOS, Fedora
# pkg macOS
