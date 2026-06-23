/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 获取目录中所有文件的遍历器
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2026年6月13日
 */
final class FileSizeVisitor extends SimpleFileVisitor<Path> {

	private long total = 0;

	public FileSizeVisitor reset() {
		total = 0;
		return this;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		total += attrs.size();
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		return FileVisitResult.CONTINUE;
	}

	public long total() {
		return total;
	}
}