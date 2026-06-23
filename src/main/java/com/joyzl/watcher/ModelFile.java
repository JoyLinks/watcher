/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.util.regex.Pattern;

/**
 * 模型文件
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月22日
 */
final class ModelFile {

	private final String example;
	private final Pattern name;
	private final Pattern code;

	public ModelFile(String name, String code, String example) {
		this.example = example;
		this.name = Pattern.compile(name);
		this.code = Pattern.compile(code);
	}

	public Pattern code() {
		return code;
	}

	public Pattern name() {
		return name;
	}

	public String example() {
		return example;
	}
}