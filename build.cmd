@ECHO OFF

java --version
ECHO=

CALL mvn --version
ECHO=

IF EXIST publish RD /S /Q publish

CALL mvn -f pom.xml clean package -U

SET VERSION=1.1.3
SET ARCH=%PROCESSOR_ARCHITECTURE%

ECHO Build executable JOYZL Archive Watcher
REM https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html

IF EXIST publish\joyzl-watcher RD /S /Q publish\joyzl-watcher

REM 构建运行镜像
jpackage ^
	--name watcher^
	--type app-image^
	--app-version %VERSION%^
	--vendor www.joyzl.com^
	--copyright www.joyzl.com^
	--description "JOYZL Archive Watcher"^
	--icon publish\watcher\watcher.ico^
	--dest publish\joyzl-watcher^
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

REM watcher -> joyzl-watcher
XCOPY publish\joyzl-watcher\watcher publish\joyzl-watcher /E /Q
RMDIR publish\joyzl-watcher\watcher /S /Q

MD publish\joyzl-watcher\patterns
COPY publish\watcher\*.xml publish\joyzl-watcher\patterns\
COPY publish\watcher\watcher.properties publish\joyzl-watcher\watcher.properties
COPY publish\watcher\autostart.ps1 publish\joyzl-watcher\autostart.ps1
COPY publish\watcher\readme.md publish\joyzl-watcher\readme.md

jar cfM publish\joyzl-watcher_windows-%ARCH%_%VERSION%.zip -C publish joyzl-watcher

PAUSE
