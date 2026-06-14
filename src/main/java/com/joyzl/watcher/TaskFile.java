/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件上传任务
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月22日
 */
public class TaskFile {

	private final Path file;
	private final String code;
	private final long time;
	private long size;

	public TaskFile(Path file, String code) throws IOException {
		this.time = System.currentTimeMillis();
		this.file = file;
		this.code = code;
	}

	public boolean exists() {
		return Files.exists(file);
	}

	public boolean readable() {
		return Files.isReadable(file);
	}

	public boolean stabled() throws IOException {
		// 检查文件在持续写入
		final long s = Files.size(file);
		if (s > 0 && size == s) {
			return true;
		} else {
			size = s;
			return false;
		}
	}

	public boolean canOpen() throws IOException {
		// 检查文件是否可打开，某些程序可能会独占文件
		try (FileInputStream input = new FileInputStream(file.toFile())) {
			return input.read() >= 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return file.toString();
	}

	public long time() {
		return time;
	}

	public String code() {
		return code;
	}

	public Path file() {
		return file;
	}

	public long size() {
		return size;
	}
}