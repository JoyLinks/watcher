/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 冲突避免，避免启动多个程序监视相同目录
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月29日
 */
public class Avoidance {

	static Path file() {
		final String dir = System.getProperty("java.io.tmpdir");
		return Path.of(dir, "watcher.lock");
	}

	static boolean register(Path watch) throws IOException {
		// 防止重复启动程序监视相同目录
		// 这可能导致上传重复的文件
		final long pid = ProcessHandle.current().pid();
		try (final FileChannel channel = FileChannel.open(file(), //
			StandardOpenOption.CREATE, //
			StandardOpenOption.WRITE, //
			StandardOpenOption.READ)) {
			try (final FileLock lock = channel.lock()) {
				final List<String> paths = readLines(channel);

				// 程序启动
				// 判断目录是否已被监视
				// 注意父目录被监视子目录也不能再监视
				Path path;
				for (String line : paths) {
					if (isAlive(getPID(line))) {
						path = Path.of(getPath(line));
						if (watch.equals(path) || watch.startsWith(path) || path.startsWith(watch)) {
							return false;
						}
					}
				}

				// [PID]:[PATH]
				paths.add(pid + ":" + watch.toAbsolutePath().toString());

				// 重写文件
				writeLines(channel, paths);
				return true;
			}
		}
	}

	static void remove(Path watch) {
		final long pid = ProcessHandle.current().pid();
		try (final FileChannel channel = FileChannel.open(file(), //
			StandardOpenOption.CREATE, //
			StandardOpenOption.WRITE, //
			StandardOpenOption.READ)) {
			try (final FileLock lock = channel.lock()) {
				final List<String> paths = readLines(channel);
				final List<String> news = new ArrayList<>();

				final String current = pid + ":" + watch.toAbsolutePath().toString();
				for (String line : paths) {
					if (line.equals(current)) {
						continue;
					}
					if (isAlive(getPID(line))) {
						paths.add(line);
					}
				}

				// 重写文件
				writeLines(channel, news);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static boolean change(Path previous, Path current) {
		final long pid = ProcessHandle.current().pid();
		try (final FileChannel channel = FileChannel.open(file(), //
			StandardOpenOption.CREATE, //
			StandardOpenOption.WRITE, //
			StandardOpenOption.READ)) {
			try (final FileLock lock = channel.lock()) {
				final List<String> paths = readLines(channel);

				// 判断目录是否已被监视
				// 注意父目录被监视子目录也不能再监视

				long id;
				Path path;
				for (String line : paths) {
					id = getPID(line);
					if (id == pid) {
						// 忽略自己的标识
						continue;
					}
					if (isAlive(id)) {
						path = Path.of(getPath(line));
						if (current.equals(path) || current.startsWith(path) || path.startsWith(current)) {
							return false;
						}
					}
				}

				// [PID]:[PATH]
				if (previous != null) {
					paths.remove(pid + ":" + previous.toAbsolutePath().toString());
				}
				paths.add(pid + ":" + current.toAbsolutePath().toString());

				// 重写文件
				writeLines(channel, paths);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 从通道读取所有行，忽略空行，不含换行符
	 */
	private static List<String> readLines(FileChannel channel) throws IOException {
		final List<String> lines = new ArrayList<>();
		if (channel.size() > 0) {
			channel.position(0);
			final ByteBuffer bytes = ByteBuffer.allocate((int) channel.size());
			channel.read(bytes);
			final CharBuffer chars = StandardCharsets.UTF_8.decode(bytes.flip());
			final String[] items = chars.toString().split("\n");
			for (String item : items) {
				item = item.trim();
				if (item.isEmpty()) {
					continue;
				}
				lines.add(item);
			}
		}
		return lines;
	}

	private static void writeLines(FileChannel channel, List<String> lines) throws IOException {
		channel.truncate(0);
		if (lines.size() > 0) {
			final String content = String.join("\n", lines) + "\n";
			final ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
		}
		channel.force(true);
	}

	/** [PID]:[PATH] */
	private static long getPID(String line) {
		int i = line.indexOf(':');
		if (i > 0) {
			return Long.parseUnsignedLong(line, 0, i, 10);
		}
		return -1;
	}

	/** [PID]:[PATH] */
	private static String getPath(String line) {
		int i = line.indexOf(':');
		if (i > 0) {
			return line.substring(i + 1);
		}
		return null;
	}

	private static boolean isAlive(long pid) {
		if (pid > 0) {
			try {
				// Java 9+
				return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
			} catch (Exception e) {
				return true;
			}
		}
		return false;
	}
}