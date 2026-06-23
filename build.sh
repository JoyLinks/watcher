#!/bin/sh

java --version
echo

mvn --version
echo

mvn -f pom.xml clean package -U

VERSION=1.1.3
ARCH=$(arch)

# 移除目标目录
rm -rf publish/joyzl-watcher

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
	--dest publish/joyzl-watcher\
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

# watcher -> joyzl-watcher
mv publish/joyzl-watcher/watcher/* publish/joyzl-watcher/
rmdir publish/joyzl-watcher/watcher

mkdir publish/joyzl-watcher/patterns
cp publish/watcher/*.xml publish/joyzl-watcher/patterns/
cp publish/watcher/watcher.properties publish/joyzl-watcher/watcher.properties
cp publish/watcher/readme.md publish/joyzl-watcher/readme.md
cp publish/watcher/install.sh publish/joyzl-watcher/install.sh

# 可执行文件
chmod +x publish/joyzl-watcher/install.sh

# 创建压缩包
tar -czf "publish/joyzl-watcher_linux-${ARCH}_${VERSION}.tar.gz" -C publish joyzl-watcher


# 创建安装包
cd publish
jpackage \
	--about-url http://www.joyzl.com\
	--app-image joyzl-watcher\
	--install-dir /opt/joyzl/watcher\
	--resource-dir joyzl-watcher\
	--linux-shortcut

# deb Ubuntu, Debian
# rpm RHEL, CentOS, Fedora
# pkg macOS
