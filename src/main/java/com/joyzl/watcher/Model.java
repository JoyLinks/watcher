/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import static com.joyzl.network.Utility.noEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.joyzl.codec.XMLElementType;
import com.joyzl.codec.XMLReader;

/**
 * 文件模式匹配模型，每个模型可包含多项文件
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月22日
 */
public class Model {

	private final String name;
	private final List<ModelFile> files = new ArrayList<>();
	private Path path;

	public Model(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public List<ModelFile> files() {
		return files;
	}

	public String name() {
		return name;
	}

	public Path path() {
		return path;
	}

	public static List<Model> loads(Path path) throws IOException {
		final List<Model> molds = new ArrayList<>();
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				if (name.endsWith(".xml")) {
					Model m = load(file);
					if (m != null) {
						molds.add(m);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return molds;
	}

	public static Model load(Path path) throws IOException {
		Model model = null;
		try (final InputStream input = Files.newInputStream(path);) {
			final XMLReader reader = new XMLReader(input);
			while (reader.nextElement()) {
				if (reader.type() == XMLElementType.NORMAL) {
					if (reader.isName("model")) {
						model = readModel(reader);
						model.path = path;
						break;
					}
				}
			}
		}
		return model;
	}

	private static Model readModel(XMLReader reader) throws IOException {
		String name = null;
		ModelFile file;
		final List<ModelFile> files = new ArrayList<>();
		final int depth = reader.depth();
		while (reader.nextElement() && reader.depth() > depth) {
			if (reader.type() == XMLElementType.NORMAL) {
				if (reader.isName("name")) {
					name = reader.getContent();
				} else if (reader.isName("file")) {
					file = readModelFile(reader);
					if (file != null) {
						files.add(file);
					}
				}
			}
		}
		if (noEmpty(name) && !files.isEmpty()) {
			Model model = new Model(name);
			model.files().addAll(files);
			return model;
		}
		return null;
	}

	private static ModelFile readModelFile(XMLReader reader) throws IOException {
		String name = null, code = null, example = null;
		final int depth = reader.depth();
		while (reader.nextElement() && reader.depth() > depth) {
			if (reader.type() == XMLElementType.NORMAL) {
				if (reader.isName("name")) {
					name = reader.getContent();
				} else if (reader.isName("code")) {
					code = reader.getContent();
				} else if (reader.isName("example")) {
					example = reader.getContent();
				}
			}
		}
		if (noEmpty(code)) {
			return new ModelFile(name, code, example);
		}
		return null;
	}
}