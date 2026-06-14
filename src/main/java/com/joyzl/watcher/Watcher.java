/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import com.joyzl.logger.Logger;

/**
 * 文件监视上传
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月21日
 */
public class Watcher extends Thread {

	private final WatchService watch;
	private final Map<Path, WatchKey> keys = new HashMap<>();
	private final Uploader uploader;
	private final Model model;
	private final Path path;

	/** 发现并匹配的文件数量 */
	private volatile int size = 0;

	public Watcher(Uploader uploader, Model model, Path path) throws IOException {
		watch = FileSystems.getDefault().newWatchService();
		register(this.path = path);
		this.uploader = uploader;
		this.model = model;
	}

	private void register(Path path) throws IOException {
		// 遍历所有文件夹，包含最外层
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Logger.debug("WATCH ", dir);
					keys.put(dir, dir.register(watch, //
						StandardWatchEventKinds.ENTRY_CREATE, //
						StandardWatchEventKinds.ENTRY_DELETE, //
						StandardWatchEventKinds.OVERFLOW));
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}
		});
	}

	public void close() {
		try {
			watch.close();
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			keys.clear();
		}
		interrupt();
	}

	public void run() {
		Path relative, absolute;
		WatchKey key;
		try {
			// 每个被监视目录响应内部变化事件
			// 不会触发被监视目录自身的变化
			// 不会触发子目录内的变化
			while ((key = watch.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						relative = (Path) event.context();
						if (relative != null) {
							absolute = ((Path) key.watchable()).resolve(relative);
							try {
								if (Files.isDirectory(absolute)) {
									register(absolute);
								} else {
									// 如果有目录确保相对路径包含目录
									relative = absolute.subpath(path.getNameCount(), absolute.getNameCount());
									match(absolute, relative);
								}
							} catch (IOException e) {
								Logger.error(e);
							}
						}
					} else

					if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
						relative = (Path) event.context();
						if (relative != null) {
							absolute = ((Path) key.watchable()).resolve(relative);
							final WatchKey k = keys.remove(absolute);
							if (k != null) {
								k.cancel();
							}
						}
					}
				}
				key.reset();
			}
		} catch (ClosedWatchServiceException e) {
			// Logger.error(e);
			return;
		} catch (InterruptedException e) {
			// Logger.error(e);
			return;
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	/** a绝对路径 r相对路径 */
	private void match(Path absolute, Path relative) throws IOException {
		final String p = relative.toString();
		// Logger.debug(p);
		Matcher matcher;
		for (ModelFile file : model.files()) {
			matcher = file.name().matcher(p);
			if (matcher.find()) {
				matcher = file.code().matcher(p);
				if (matcher.find() && matcher.groupCount() > 0) {
					final String code = matcher.group(1);
					final TaskFile task = new TaskFile(absolute, code);
					Logger.debug("MATCH ", code, " ", p);
					uploader.add(task);
					size++;
					break;
				}
			}
		}
	}

	public Path path() {
		return path;
	}

	public int size() {
		return size;
	}
}