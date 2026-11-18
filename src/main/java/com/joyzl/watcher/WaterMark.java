package com.joyzl.watcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 水位线（指示已上传文件的最后时间戳）
 * 
 * @author ZhangXi 2026年11月18日
 */
public class WaterMark {

	private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	private final FileChannel channel;
	private volatile long timestamp;

	/*-
	 * 水位线机制
	 * 每当文件上传完成更新水位线为文件最后修改时间并立即保存
	 * 程序启动时根据水位线检查目录和文件的最后修改时间
	 * 早于水位线则已上传，晚于水位线则未上传
	 * 
	 * 文件夹的修改时间在以下情形更新
	 * 文件夹中新建文件或文件夹
	 * 文件夹中重命名直接子项
	 * 文件夹中删除直接子项
	 * 不会更新父文件夹
	 */

	public WaterMark() throws IOException {
		channel = FileChannel.open(Path.of("watermark.time"), //
			StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		try {
			buffer.clear();
			while (buffer.position() < Long.BYTES) {
				if (channel.read(buffer) <= 0) {
					break;
				}
			}

			final long now = System.currentTimeMillis();
			if (buffer.position() == Long.BYTES) {
				timestamp = buffer.flip().getLong();
				if (timestamp > 0 && timestamp <= now + 86_400_000L) {
					// 容忍1天以内的时钟漂移
					return;
				} else {
					timestamp = now;
				}
			} else {
				timestamp = now;
			}

			buffer.clear();
			buffer.putLong(timestamp);
			buffer.flip();

			channel.position(0);
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
			channel.truncate(Long.BYTES);
			channel.force(true);
		} catch (IOException e) {
			try {
				channel.close();
			} catch (IOException suppressed) {
				e.addSuppressed(suppressed);
			}
			throw e;
		}
	}

	public synchronized void update(long value) throws IOException {
		buffer.clear();
		buffer.putLong(value);
		buffer.flip();

		channel.position(0);
		while (buffer.hasRemaining()) {
			channel.write(buffer);
		}
		channel.truncate(Long.BYTES);
		channel.force(true);
		timestamp = value;
	}

	public synchronized void close() throws IOException {
		if (channel.isOpen()) {
			buffer.clear();
			buffer.putLong(timestamp);
			buffer.flip();

			try {
				channel.position(0);
				while (buffer.hasRemaining()) {
					channel.write(buffer);
				}
				channel.truncate(Long.BYTES);
				channel.force(true);
			} catch (IOException e) {
				try {
					channel.close();
				} catch (IOException suppressed) {
					e.addSuppressed(suppressed);
				}
				throw e;
			}
		}
	}

	public long timestamp() {
		return timestamp;
	}
}