module com.joyzl.watcher {

	requires com.joyzl.logger;
	requires com.joyzl.network;
	requires com.joyzl.backdoor;
	requires java.desktop;

	// Apache Commons Daemon 以 jvm 模式启动停止服务时需要
	exports com.joyzl.watcher;
}