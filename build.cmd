@ECHO OFF

java --version
ECHO=

CALL mvn --version
ECHO=

IF EXIST publish RD /S /Q publish

CALL mvn -f pom.xml clean package -U

SET VERSION=1.2.0
SET ARCH=%PROCESSOR_ARCHITECTURE%

ECHO Build executable JOYZL Archive Watcher
REM https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html

REM 构建运行镜像
jpackage ^
	--name watcher^
	--type app-image^
	--app-version %VERSION%^
	--vendor www.joyzl.com^
	--copyright www.joyzl.com^
	--description "JOYZL Archive Watcher"^
	--icon publish\watcher\watcher.ico^
	--dest publish\joyzl-archive-watcher^
	--module-path publish\watcher\lib^
	--module com.joyzl.watcher/com.joyzl.watcher.Application^
	--add-modules jdk.charsets^
	--add-modules jdk.localedata^
	--jlink-options --compress=2^
	--jlink-options --no-header-files^
	--jlink-options --no-man-pages^
	--jlink-options --bind-services^
	--jlink-options --include-locales=zh-cn^
	--java-options -Xms256m^
	--java-options -Xmx1024m^
	--java-options -Dfile.encoding=UTF-8^
	--java-options -Duser.timezone=GMT+08^
	--verbose

REM publish\joyzl-archive-watcher\watcher -> publish\joyzl-archive-watcher
XCOPY publish\joyzl-archive-watcher\watcher publish\joyzl-archive-watcher /E /Q
RMDIR publish\joyzl-archive-watcher\watcher /S /Q

MD publish\joyzl-archive-watcher\patterns
COPY publish\watcher\*.xml publish\joyzl-archive-watcher\patterns\
COPY publish\watcher\service.exe publish\joyzl-archive-watcher\service.exe
COPY publish\watcher\watcher.properties publish\joyzl-archive-watcher\watcher.properties
COPY publish\watcher\install-desktop.ps1 publish\joyzl-archive-watcher\install-desktop.ps1
COPY publish\watcher\install-service.ps1 publish\joyzl-archive-watcher\install-service.ps1
COPY publish\watcher\uninstall.ps1 publish\joyzl-archive-watcher\uninstall.ps1
COPY publish\watcher\update.ps1 publish\joyzl-archive-watcher\update.ps1
COPY publish\watcher\readme.md publish\joyzl-archive-watcher\readme.md

jar cfM publish\joyzl-archive-watcher_windows-%ARCH%_%VERSION%.zip -C publish joyzl-archive-watcher

PAUSE
