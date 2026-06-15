/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import com.joyzl.logger.Logger;
import com.joyzl.network.Point;
import com.joyzl.network.Utility;
import com.joyzl.network.buffer.DataBuffer;
import com.joyzl.network.chain.ChainChannel;
import com.joyzl.network.chain.ChainGenericsHandler;
import com.joyzl.network.chain.UDPServer;
import com.joyzl.network.chain.UDPSlave;
import com.joyzl.network.codec.KeyValueCoder;

/**
 * 基于UDP远程发现与配置
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2026年1月7日
 */
public final class Backdoor {

	private UDPServer server;
	private Setting setting;

	public void reset(Setting setting) throws IOException {
		this.setting = setting;
		final String point = Utility.noEmpty(setting.getUDP()) ? setting.getUDP() : "1030";
		final int port = Point.getPort(point);
		final String host = Point.getHost(point);
		if (server != null) {
			if (server.active()) {
				final InetSocketAddress address;
				if (host == null || host.length() == 0) {
					address = new InetSocketAddress(port);
				} else {
					address = new InetSocketAddress(host, port);
				}
				if (address.equals(server.getLocalAddress())) {
					return;
				}
			}
			close();
		}
		server = new UDPServer(new Handler(), host, port > 0 ? port : 1030);
		server.receive();
	}

	public void close() {
		if (server != null) {
			server.close();
			server = null;
		}
	}

	private class Handler implements ChainGenericsHandler<UDPSlave, Object> {

		@Override
		public void connected(UDPSlave slave) throws Exception {
		}

		@Override
		public Object decode(UDPSlave slave, DataBuffer buffer) throws Exception {
			final byte value = buffer.backByte();
			if (value == 0x0D || value == 0x0A) {
				return new String(buffer.readASCIIs(buffer.readable()));
			} else if (value == 0x05) {
				final Parameters parameters = new Parameters();
				KeyValueCoder.decode(parameters, buffer);
				return parameters;
			} else {
				buffer.clear();
				return null;
			}
		}

		@Override
		public void received(UDPSlave slave, Object message) throws Exception {
			if (message != null) {
				// SETTING
				if (message instanceof Parameters parameters) {
					if (parameters.isEmpty()) {
					} else {
						setting.update(parameters);
						Application.reset();
						setting.save();
					}
					setting.extract(parameters);
					slave.send(parameters);
				}
			}
		}

		@Override
		public DataBuffer encode(UDPSlave slave, Object message) throws Exception {
			final DataBuffer buffer = DataBuffer.instance();
			if (message instanceof String) {
				buffer.writeASCIIs(message.toString());
			} else if (message instanceof Parameters parameters) {
				KeyValueCoder.encode(parameters, buffer);
				buffer.writeByte(0x05);
			} else {
				throw new UnsupportedOperationException(message.getClass().toString());
			}
			return buffer;
		}

		@Override
		public void sent(UDPSlave slave, Object message) throws Exception {
		}

		@Override
		public void disconnected(UDPSlave chain) throws Exception {
		}

		@Override
		public void error(ChainChannel chain, Throwable e) {
			Logger.error(e);
		}

		@Override
		public void beat(UDPSlave slave) throws Exception {
		}

		@Override
		public void error(UDPSlave chain, Throwable e) {
			Logger.error(e);
		}
	}

	// 避免泛型警告
	static class Parameters extends HashMap<String, String> {
		private static final long serialVersionUID = 1L;
	}
}