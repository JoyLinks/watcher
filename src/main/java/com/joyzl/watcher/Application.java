/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import static com.joyzl.network.Utility.noEmpty;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.joyzl.backdoor.Backdoor;
import com.joyzl.backdoor.Parameters;
import com.joyzl.backdoor.ReceivedCallback;
import com.joyzl.logger.Logger;
import com.joyzl.logger.LoggerCleaner;
import com.joyzl.logger.LoggerService;
import com.joyzl.network.Executor;
import com.joyzl.network.chain.UDPSlave;

/**
 * 文件归档客户端
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月21日
 */
public class Application {

	private final static Setting setting = new Setting();
	private final static Backdoor backdoor = new Backdoor();
	private static Uploader uploader;
	private static Watcher watcher;
	private static Model model;
	private static Thread main;

	public static void main(String[] args) {
		start(args);
	}

	public static void start(String[] args) {
		try {
			// 加载配置
			setting.load();

			// 日志输出级别
			Logger.setFile(setting.getLogPath(), null, ".log");
			LoggerService.setExpires(setting.getLogExpires());
			Logger.setLevel(setting.getLogLevel());
			Logger.info("START");

			// 防止重复监视启动
			if (noEmpty(setting.getWatch())) {
				final Path path = Path.of(setting.getWatch());
				if (Files.exists(path)) {
					if (!Avoidance.register(path)) {
						Window.error("监视程序已在运行中\n" + path);
						Logger.error("监视程序已在运行中 ", path);
						stop(args);
						return;
					}
				}
			}

			Runtime.getRuntime().addShutdownHook(new Thread("SHUTDOWN") {
				@Override
				public void run() {
					Application.stop(null);
				}
			});

			Executor.initialize(8);
			reset();

			Tray.show();
			Window.show();

			daemon();
		} catch (Exception e) {
			Window.error(e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	public static void reset() throws IOException {
		if (setting.getUDP() != null) {
			backdoor.reset(setting.getUDP());
			backdoor.setOnSetting(new ReceivedCallback<>() {
				@Override
				public void received(UDPSlave slave, Parameters parameters) {
					if (parameters.isEmpty()) {
						setting.extract(parameters);
					} else {
						setting.update(parameters);
						setting.extract(parameters);
						try {
							Application.reset();
							setting.save();
						} catch (Exception e) {
							Logger.error(e);
						}
					}
				}
			});
		} else {
			backdoor.close();
		}

		if (uploader != null) {
			uploader.close();
			uploader = null;
		}
		if (noEmpty(setting.getHTTPServer())) {
			try {
				uploader = new Uploader(setting.getHTTPServer(), setting.getNumber(), setting.getUploadExpires(), setting.getStableExpires());
				uploader.test();
			} catch (Exception e) {
				Window.error(e.getMessage());
				Logger.error(e);
			}
		}

		model = null;
		if (noEmpty(setting.getModel())) {
			final Path path = Path.of(setting.getModel());
			if (Files.notExists(path)) {
				Window.error("匹配模型不存在:" + path);
				Logger.error("匹配模型不存在:", path);
			} else {
				try {
					model = Model.load(path);
					if (model == null) {
						Window.error("匹配模型加载失败");
						Logger.error("匹配模型加载失败");
					}
				} catch (IOException e) {
					Window.error(e.getMessage());
					Logger.error(e);
				}
			}
		}

		if (watcher != null) {
			watcher.close();
			watcher = null;
		}
		if (noEmpty(setting.getWatch())) {
			final Path path = Path.of(setting.getWatch());
			if (Files.exists(path)) {
				if (uploader != null && model != null) {
					try {
						watcher = new Watcher(uploader, model, path);
						watcher.start();
					} catch (AccessDeniedException e) {
						Window.error("无权访问:" + e.getMessage());
						Logger.error("无权访问:", e.getMessage());
					} catch (IOException e) {
						Window.error(e.getMessage());
						Logger.error(e);
					}
				}
			} else {
				Window.error("目录不存在:" + path);
				Logger.error("目录不存在:", path);
			}
		}
	}

	private static void daemon() {
		main = Thread.currentThread();
		try {
			while (watcher != null && uploader != null) {
				if (LoggerService.last(60 * 1000)) {
					final LoggerCleaner cleaaner = LoggerService.clean();
					Logger.info(cleaaner);
				}
				Thread.sleep(60 * 1000);
			}
		} catch (InterruptedException e) {
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public static void stop(String[] args) {
		Logger.info("STOP");
		try {
			if (watcher != null) {
				Avoidance.remove(watcher.path());
				watcher.close();
				watcher = null;
			}
			if (uploader != null) {
				uploader.close();
				uploader = null;
			}
			if (main != null) {
				main.interrupt();
				main = null;
			}
		} catch (Exception e) {
			Logger.error(e);
		} finally {
			Tray.close();
			Window.close();
			Executor.shutdown();
		}
	}

	static Setting setting() {
		return setting;
	}

	static Uploader uploader() {
		return uploader;
	}

	static Watcher watcher() {
		return watcher;
	}

	static Model model() {
		return model;
	}
}