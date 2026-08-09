/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.File;
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
import java.nio.file.attribute.BasicFileAttributes;
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
final class Watcher extends Thread {

	private final WatchService watch;
	private final Uploader uploader;
	private final Model model;
	private final Path path;

	/** 发现并匹配的文件数 */
	private int size = 0;

	public Watcher(Uploader uploader, Model model, Path path) throws IOException {
		watch = FileSystems.getDefault().newWatchService();
		this.uploader = uploader;
		this.model = model;
		this.path = path;
	}

	private final Map<Path, WatchKey> watchs = new HashMap<>();
	private final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
		// 处理监视后创建的目录
		// 遍历其中的所有子目录和文件
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc == null) {
				Logger.debug("WATCH ", dir);
				watchs.put(dir, dir.register(watch, //
					StandardWatchEventKinds.ENTRY_CREATE, //
					StandardWatchEventKinds.ENTRY_DELETE, //
					StandardWatchEventKinds.OVERFLOW));
				return FileVisitResult.CONTINUE;
			} else {
				throw exc;
			}
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			final Path relative = file.subpath(path.getNameCount(), file.getNameCount());
			match(file, relative);
			return FileVisitResult.CONTINUE;
		}
	};

	public void run() {
		// 每个被监视目录响应内部变化事件
		// 不会触发被监视目录自身的变化
		// 不会触发子目录内的变化
		try {
			// 遍历所有文件夹，包含最外层
			Files.walkFileTree(path, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (exc == null) {
						// 过滤匹配目录，不含文件名
						// 这些目录是历史结果性的，特别是按目录匹配时
						Matcher matcher;
						for (ModelFile file : model.files()) {
							matcher = file.name().matcher(dir.toString());
							if (matcher.find()) {
								return FileVisitResult.CONTINUE;
							}
						}

						Logger.debug("WATCH ", dir);
						watchs.put(dir, dir.register(watch, //
							StandardWatchEventKinds.ENTRY_CREATE, //
							StandardWatchEventKinds.ENTRY_DELETE, //
							StandardWatchEventKinds.OVERFLOW));
						return FileVisitResult.CONTINUE;
					} else {
						throw exc;
					}
				}
			});

			// 阻塞线程等待事件
			WatchKey key;
			Path relative, absolute, last = path;
			while ((key = watch.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						relative = (Path) event.context();
						if (relative != null) {
							absolute = ((Path) key.watchable()).resolve(relative);
							try {
								if (Files.isDirectory(absolute)) {
									// Logger.debug("WATCH ", absolute);
									// watchs.put(absolute,
									// absolute.register(watch, //
									// StandardWatchEventKinds.ENTRY_CREATE, //
									// StandardWatchEventKinds.ENTRY_DELETE, //
									// StandardWatchEventKinds.OVERFLOW));

									// 如果目录是移动而来，其中的文件不会触发事件
									// 以下遍历将包括最外层目录，无须单独处理外层
									Files.walkFileTree(absolute, visitor);
								} else {
									// 如果有目录确保相对路径包含目录
									relative = absolute.subpath(path.getNameCount(), absolute.getNameCount());
									if (last.equals(relative)) {
										// 经测试存在重复创建同一个文件的情况
										// 因此须执行排重，避免相同文件多次上传
										continue;
									} else {
										match(absolute, relative);
										last = relative;
									}
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
							final WatchKey k = watchs.remove(absolute);
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
	private void match(Path absolute, Path relative) {
		final String p = relative.toString();
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

					// 允许多个正则表达式匹配，因此不中断
					// 例如二码合一时，要按单个码提取时
					// break;
				}
			}
		}
	}

	public void match(File file) throws IOException {
		final Path absolute = file.toPath();
		if (absolute.startsWith(path)) {
			// 位于被监视目录中
			if (file.isDirectory()) {
				Files.walkFileTree(absolute, new SimpleFileVisitor<>() {
					// 遍历其中的所有文件
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						final Path relative = file.subpath(path.getNameCount(), file.getNameCount());
						match(file, relative);
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				final Path relative = absolute.subpath(path.getNameCount(), absolute.getNameCount());
				match(absolute, relative);
			}
		} else {
			// 位于被监视目录外
			if (file.isDirectory()) {
				Files.walkFileTree(absolute, new SimpleFileVisitor<>() {
					// 遍历其中的所有文件
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						match(file, file);
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				match(absolute, absolute);
			}
		}
	}

	public void close() {
		try {
			watch.close();
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			watchs.clear();
		}
		interrupt();
	}

	public Path path() {
		return path;
	}

	public int size() {
		return size;
	}
}