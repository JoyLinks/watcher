/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import static com.joyzl.network.Utility.noEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * 设置参数
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年8月21日
 */
final class Setting {

	private int thread = 8;
	/** BACK DOOR */
	private String udp;
	private String number;
	private String httpServer;
	private String model;
	private String watch;
	private int uploadExpires = 3600;
	private int stableExpires = 3600;
	private int logExpires = 30;
	/** 日志位置 */
	private String logPath = "log";
	private int logLevel = 3;

	public int getThread() {
		return thread;
	}

	public void setThread(int value) {
		thread = value;
	}

	public String getUDP() {
		return udp;
	}

	public void setUDP(String value) {
		udp = noEmpty(value) ? value : null;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String value) {
		number = noEmpty(value) ? value : null;
	}

	public String getHTTPServer() {
		return httpServer;
	}

	public void setHTTPServer(String value) {
		httpServer = noEmpty(value) ? value : null;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String value) {
		model = noEmpty(value) ? value : null;
	}

	public String getWatch() {
		return watch;
	}

	public void setWatch(String value) {
		watch = noEmpty(value) ? value : null;
	}

	public int getUploadExpires() {
		return uploadExpires;
	}

	public void setUploadExpires(int value) {
		uploadExpires = value;
	}

	public int getStableExpires() {
		return stableExpires;
	}

	public void setStableExpires(int value) {
		stableExpires = value;
	}

	public int getLogExpires() {
		return logExpires;
	}

	public void setLogExpires(int value) {
		logExpires = value;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String value) {
		logPath = noEmpty(value) ? value : "log";
	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(int value) {
		logLevel = value;
	}

	public void load() throws IOException {
		final File file = new File("watcher.properties");
		final Properties properties = new Properties();
		if (file.exists()) {
			try (FileInputStream input = new FileInputStream(file)) {
				properties.load(input);

				udp = properties.getProperty("UDP");
				model = properties.getProperty("MODEL");
				watch = properties.getProperty("WATCH");
				number = properties.getProperty("NUMBER");
				httpServer = properties.getProperty("HTTP_SERVER");
				logPath = properties.getProperty("LOG_PATH");

				String temp = properties.getProperty("UPLOAD_EXPIRES");
				if (noEmpty(temp)) {
					try {
						uploadExpires = Integer.parseUnsignedInt(temp);
					} catch (Exception e) {
						uploadExpires = 0;
					}
				}
				temp = properties.getProperty("STABLE_EXPIRES");
				if (noEmpty(temp)) {
					try {
						stableExpires = Integer.parseUnsignedInt(temp);
					} catch (Exception e) {
						stableExpires = 0;
					}
				}
				temp = properties.getProperty("LOG_EXPIRES");
				if (noEmpty(temp)) {
					try {
						logExpires = Integer.parseUnsignedInt(temp);
					} catch (Exception e) {
						logExpires = 30;
					}
				}
				temp = properties.getProperty("LOG_LEVEL");
				if (noEmpty(temp)) {
					try {
						logLevel = Integer.parseUnsignedInt(temp);
					} catch (Exception e) {
						logLevel = 1;
					}
				}
				temp = properties.getProperty("THREAD");
				if (noEmpty(temp)) {
					try {
						thread = Integer.parseUnsignedInt(temp);
					} catch (Exception e) {
						thread = 0;
					}
				}
			}
		}
	}

	public void update(Map<String, String> parameters) {
		if (parameters.containsKey("UDP")) {
			udp = parameters.get("UDP");
		}
		if (parameters.containsKey("MODEL")) {
			model = parameters.get("MODEL");
		}
		if (parameters.containsKey("WATCH")) {
			watch = parameters.get("WATCH");
		}
		if (parameters.containsKey("NUMBER")) {
			number = parameters.get("NUMBER");
		}
		if (parameters.containsKey("HTTP_SERVER")) {
			httpServer = parameters.get("HTTP_SERVER");
		}
		if (parameters.containsKey("LOG_PATH")) {
			logPath = parameters.get("LOG_PATH");
		}
		if (parameters.containsKey("UPLOAD_EXPIRES")) {
			try {
				uploadExpires = Integer.parseUnsignedInt(parameters.get("UPLOAD_EXPIRES"));
			} catch (Exception e) {
				uploadExpires = 0;
			}
		}
		if (parameters.containsKey("STABLE_EXPIRES")) {
			try {
				stableExpires = Integer.parseUnsignedInt(parameters.get("STABLE_EXPIRES"));
			} catch (Exception e) {
				stableExpires = 0;
			}
		}
		if (parameters.containsKey("LOG_EXPIRES")) {
			try {
				logExpires = Integer.parseUnsignedInt(parameters.get("LOG_EXPIRES"));
			} catch (Exception e) {
				logExpires = 30;
			}
		}
		if (parameters.containsKey("LOG_LEVEL")) {
			try {
				logLevel = Integer.parseUnsignedInt(parameters.get("LOG_LEVEL"));
			} catch (Exception e) {
				logLevel = 1;
			}
		}
		if (parameters.containsKey("THREAD")) {
			try {
				thread = Integer.parseUnsignedInt(parameters.get("THREAD"));
			} catch (Exception e) {
				thread = 0;
			}
		}
	}

	public void save() throws IOException {
		final File setting = new File("watcher.properties");
		final Properties properties = new Properties();
		if (udp != null) {
			properties.setProperty("UDP", udp);
		}
		if (model != null) {
			properties.setProperty("MODEL", model);
		}
		if (watch != null) {
			properties.setProperty("WATCH", watch);
		}
		if (number != null) {
			properties.setProperty("NUMBER", number);
		}
		if (httpServer != null) {
			properties.setProperty("HTTP_SERVER", httpServer);
		}
		if (logPath != null) {
			properties.setProperty("LOG_PATH", logPath);
		}

		properties.setProperty("UPLOAD_EXPIRES", Integer.toString(uploadExpires));
		properties.setProperty("STABLE_EXPIRES", Integer.toString(stableExpires));
		properties.setProperty("LOG_EXPIRES", Integer.toString(logExpires));
		properties.setProperty("LOG_LEVEL", Integer.toString(logLevel));
		properties.setProperty("THREAD", Integer.toString(thread));

		try (FileOutputStream output = new FileOutputStream(setting)) {
			properties.store(output, "JOYZL Watcher");
		}
	}

	public void extract(Map<String, String> parameters) {
		parameters.put("OS", System.getProperty("os.name"));
		parameters.put("ARCH", System.getProperty("os.arch"));
		parameters.put("VERSION", version());
		parameters.put("NAME", "WATCHER");

		if (udp != null) {
			parameters.put("UDP", udp);
		}
		if (model != null) {
			parameters.put("MODEL", model);
		}
		if (watch != null) {
			parameters.put("WATCH", watch);
		}
		if (number != null) {
			parameters.put("NUMBER", number);
		}
		if (httpServer != null) {
			parameters.put("HTTP_SERVER", httpServer);
		}
		if (logPath != null) {
			parameters.put("LOG_PATH", logPath);
		}

		parameters.put("UPLOAD_EXPIRES", Integer.toString(uploadExpires));
		parameters.put("STABLE_EXPIRES", Integer.toString(stableExpires));
		parameters.put("LOG_EXPIRES", Integer.toString(logExpires));
		parameters.put("LOG_LEVEL", Integer.toString(logLevel));
		parameters.put("THREAD", Integer.toString(thread));
	}

	public String version() {
		final Optional<Version> optional = Watcher.class.getModule().getDescriptor().version();
		if (optional.isEmpty()) {
			final String version = Watcher.class.getPackage().getImplementationVersion();
			if (version == null) {
				return System.getProperty("jpackage.app-version");
			}
		}
		return optional.get().toString();
	}
}