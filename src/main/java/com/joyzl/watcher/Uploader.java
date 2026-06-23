/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.joyzl.logger.Logger;
import com.joyzl.network.Executor;
import com.joyzl.network.http.ContentLength;
import com.joyzl.network.http.ContentType;
import com.joyzl.network.http.FormDataCoder;
import com.joyzl.network.http.HTTP1;
import com.joyzl.network.http.HTTP1ClientHandler;
import com.joyzl.network.http.HTTPClient;
import com.joyzl.network.http.HTTPStatus;
import com.joyzl.network.http.MIMEType;
import com.joyzl.network.http.MultipartFile;
import com.joyzl.network.http.MultipartFile.MultipartFiles;
import com.joyzl.network.http.Request;
import com.joyzl.network.http.Response;

/**
 * 文件上传
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月22日
 */
final class Uploader implements Runnable {

	private final String url;
	private final String host, path;
	private final int port;
	private final int uploadExpires, stableExpires;
	private volatile String number;

	private final ConcurrentLinkedQueue<TaskFile> QUEUE = new ConcurrentLinkedQueue<>();
	private final ScheduledFuture<?> future;

	public Uploader(String serverAPI, String number, int ue, int se) throws MalformedURLException {
		this.number = number;

		final URL u = new URL(url = serverAPI);
		port = u.getPort() > 0 ? u.getPort() : 80;
		host = u.getHost();
		path = u.getPath();

		uploadExpires = ue > 1 ? ue * 1000 : 60 * 60 * 1000;
		stableExpires = se > 1 ? se * 1000 : 60 * 60 * 1000;

		future = Executor.scheduleWithFixedDelay(this, 2, 2, TimeUnit.SECONDS);
		client = new HTTPClient(handler, host, port);
	}

	public void add(TaskFile task) {
		QUEUE.add(task);
	}

	public void close() {
		client.close();
		future.cancel(false);
	}

	private volatile TaskFile current;
	/** 上传的文件数量 */
	private volatile int size = 0;

	@Override
	public void run() {
		long time = System.currentTimeMillis();
		try {
			Window.refresh();

			if (current != null) {
				// 1小时未能成功上传则移除
				if (time - current.time() > uploadExpires) {
					current = null;
				} else {
					if (!client.active()) {
						client.connect();
					}
					return;
				}
			}

			if (QUEUE.isEmpty()) {
				if (client.active()) {
					client.close();
				}
			} else {
				final TaskFile task = QUEUE.peek();
				if (task.exists()) {
					if (task.readable()) {
						if (task.stabled()) {
							if (task.canOpen()) {
								current = QUEUE.poll();
								if (client.active()) {
									upload();
								} else {
									client.connect();
								}
							} else {
								// 1小时未能稳定则移除
								if (time - task.time() > stableExpires) {
									Logger.debug("文件超时被占用:" + task);
									QUEUE.poll();
								}
							}
						} else {
							// 1小时未能稳定则移除
							if (time - task.time() > stableExpires) {
								Logger.debug("文件超时未稳定:" + task);
								QUEUE.poll();
							}
						}
					} else {
						// 1小时未能稳定则移除
						if (time - task.time() > stableExpires) {
							Logger.debug("文件超时不可读:" + task);
							QUEUE.poll();
						}
					}
				} else {
					Logger.debug("文件已被删除:" + task);
					QUEUE.poll();
				}
			}
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	private final HTTPClient client;
	private final HTTP1ClientHandler handler = new HTTP1ClientHandler() {

		@Override
		public long getTimeoutRead() {
			return 60000L;
		}

		@Override
		public void connected(HTTPClient client) throws Exception {
			super.connected(client);
			upload();
		}

		@Override
		protected void received(HTTPClient client, Request request, Response response) {
			if (response.getStatus() == HTTPStatus.OK.code()) {
				Logger.debug("DONE ", current);
				current = null;
				size++;
			} else {
				Logger.debug("FAIL ", current, " ", response);
				// 错误的排后，防止一直错而导致其它耽误
				QUEUE.add(current);
				current = null;
				client.reset();
			}

			try {
				response.clearContent();
				if (request != null) {
					request.clearContent();
				}
			} catch (Exception e) {
				Logger.error(e);
			}
		}

		// TEST
		// @Override
		// public Object decode(HTTPClient client, DataBuffer buffer) throws
		// Exception {
		// System.out.println("------");
		// System.out.print(HTTP1Coder.toString(buffer));
		// System.out.println("------");
		// return super.decode(client, buffer);
		// }
		// @Override
		// public void sent(HTTPClient client, Message message) throws Exception
		// {
		// if (message.isComplete()) {
		// Logger.debug("SENT COMPLETE");
		// }
		// super.sent(client, message);
		// }
		// public Object decode(HTTPClient client, DataBuffer buffer) throws
		// Exception {
		// System.out.println(HTTP1Coder.toString(buffer));
		// Object r = super.decode(client, buffer);
		// System.out.println(buffer);
		// return r;
		// }

		@Override
		public void error(HTTPClient client, Throwable e) {
			Logger.debug(e);
			client.reset();
		}
	};

	private void upload() throws IOException {
		final TaskFile task = current;
		if (task == null) {
			return;
		}
		if (!task.exists()) {
			current = null;
			return;
		}

		final ContentType type = new ContentType();
		type.setType(MIMEType.MULTIPART_FORMDATA);
		type.setBoundary(ContentType.boundary());

		final Request request = new Request();
		request.setMethod(HTTP1.POST);
		request.setURL(path);
		request.addHeader(type);
		request.addParameter("code", task.code());
		request.addParameter("time", Long.toUnsignedString(task.time()));
		if (number != null) {
			request.addParameter("number", number);
		}

		final MultipartFiles files = new MultipartFiles();
		if (Files.isDirectory(task.file())) {
			final FileListVisitor visitor = new FileListVisitor();
			Files.walkFileTree(task.file(), visitor);
			MultipartFile file;
			for (Path f : visitor.files()) {
				file = new MultipartFile(f.toFile());
				file.setField("file");
				files.add(file);
			}
		} else {
			final MultipartFile file = new MultipartFile(task.file().toFile());
			file.setField("file");
			files.add(file);
		}
		request.setContent(files);

		FormDataCoder.write(request);
		final ContentLength length = new ContentLength();
		length.setLength(request.contentSize());
		request.addHeader(length);

		Logger.debug("UPLOAD ", task);
		client.send(request);
	}

	public void test() {
		// 连接测试
		// 启动时执行测试暴露防火墙问题
		Executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					final URL u = new URL(url);
					final HttpURLConnection connection = (HttpURLConnection) u.openConnection();
					connection.setConnectTimeout((int) (handler.getTimeoutRead() / 1000));
					Logger.debug("TEST ", connection.getResponseMessage());
				} catch (IOException e) {
					Logger.error("TEST ", e.getMessage());
				}
			}
		});
	}

	public void setNumber(String value) {
		number = value;
	}

	public String number() {
		return number;
	}

	public String url() {
		return url;
	}

	public int size() {
		return size;
	}
}