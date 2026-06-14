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
import java.util.Map;
import java.util.Properties;

/**
 * 设置参数
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年8月21日
 */
public class Setting {

	private String udp;
	private String number;
	private String httpServer;
	private String model;
	private String watch;
	private int uploadExpires;
	private int stableExpires;
	private int logExpires;
	private int logLevel;

	public String getUDP() {
		return udp;
	}

	public void setUDP(String value) {
		udp = value;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String value) {
		number = value;
	}

	public String getHTTPServer() {
		return httpServer;
	}

	public void setHTTPServer(String value) {
		httpServer = value;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String value) {
		model = value;
	}

	public String getWatch() {
		return watch;
	}

	public void setWatch(String value) {
		watch = value;
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
				number = properties.getProperty("NUMBER");
				httpServer = properties.getProperty("HTTP_SERVER");
				model = properties.getProperty("MODEL");
				watch = properties.getProperty("WATCH");

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
			}
		}
	}

	public void update(Map<String, String> parameters) {
		if (parameters.containsKey("UDP")) {
			udp = parameters.get("UDP");
		}
		if (parameters.containsKey("NUMBER")) {
			number = parameters.get("NUMBER");
		}
		if (parameters.containsKey("HTTP_SERVER")) {
			httpServer = parameters.get("HTTP_SERVER");
		}
		if (parameters.containsKey("MODEL")) {
			model = parameters.get("MODEL");
		}
		if (parameters.containsKey("WATCH")) {
			watch = parameters.get("WATCH");
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
	}

	public void save() throws IOException {
		final File setting = new File("watcher.properties");
		final Properties properties = new Properties();
		if (udp != null) {
			properties.setProperty("UDP", udp);
		}
		if (number != null) {
			properties.setProperty("NUMBER", number);
		}
		if (httpServer != null) {
			properties.setProperty("HTTP_SERVER", httpServer);
		}
		if (model != null) {
			properties.setProperty("MODEL", model);
		}
		if (watch != null) {
			properties.setProperty("WATCH", watch);
		}

		properties.setProperty("UPLOAD_EXPIRES", Integer.toString(uploadExpires));
		properties.setProperty("STABLE_EXPIRES", Integer.toString(stableExpires));
		properties.setProperty("LOG_EXPIRES", Integer.toString(logExpires));
		properties.setProperty("LOG_LEVEL", Integer.toString(logLevel));

		try (FileOutputStream output = new FileOutputStream(setting)) {
			properties.store(output, "JOYZL Watcher");
		}
	}

	public void extract(Map<String, String> parameters) {
		parameters.put("OS", System.getProperty("os.name"));
		parameters.put("ARCH", System.getProperty("os.arch"));
		parameters.put("VERSION", System.getProperty("jpackage.app-version"));

		if (udp != null) {
			parameters.put("UDP", udp);
		}
		if (number != null) {
			parameters.put("NUMBER", number);
		}
		if (httpServer != null) {
			parameters.put("HTTP_SERVER", httpServer);
		}
		if (model != null) {
			parameters.put("MODEL", model);
		}
		if (watch != null) {
			parameters.put("WATCH", watch);
		}

		parameters.put("UPLOAD_EXPIRES", Integer.toString(uploadExpires));
		parameters.put("STABLE_EXPIRES", Integer.toString(stableExpires));
		parameters.put("LOG_EXPIRES", Integer.toString(logExpires));
		parameters.put("LOG_LEVEL", Integer.toString(logLevel));
	}
}