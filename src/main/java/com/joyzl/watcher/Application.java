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

import com.joyzl.logger.Logger;
import com.joyzl.logger.LoggerService;
import com.joyzl.network.Executor;

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

	public static void main(String[] args) {
		try {
			Logger.setFile("log", null, ".log");
			// 加载配置
			setting.load();
			// 日志输出级别
			Logger.setLevel(setting.getLogLevel());
			// 日志过期天数
			LoggerService.setExpires(setting.getLogExpires());

			// 防止重复监视启动
			if (noEmpty(setting.getWatch())) {
				final Path path = Path.of(setting.getWatch());
				if (Files.exists(path)) {
					if (!Avoidance.register(path)) {
						Window.error("监视程序已在运行中\n" + path);
						close();
						return;
					}
				}
			}

			Executor.initialize(8);
			reset();
			Tray.show();
			Window.show();
		} catch (Exception e) {
			Window.error(e.getMessage());
			Logger.error(e);
		}
	}

	public static void reset() throws IOException {
		backdoor.reset(setting);

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
			} else {
				try {
					model = Model.load(path);
					if (model == null) {
						Window.error("匹配模型加载失败");
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
					} catch (IOException e) {
						Window.error(e.getMessage());
					}
				}
			} else {
				Window.error("目录不存在:" + path);
			}
		}
	}

	public static void close() {
		Logger.info("CLOSE");
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
		} catch (Exception e) {
			Logger.error(e);
		} finally {
			Tray.close();
			Window.close();
			Executor.shutdown();
		}
	}

	public static Setting setting() {
		return setting;
	}

	public static Uploader uploader() {
		return uploader;
	}

	public static Watcher watcher() {
		return watcher;
	}

	public static Model model() {
		return model;
	}
}