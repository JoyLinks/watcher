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
import com.joyzl.watcher.Backdoor.Parameters;

/**
 * 基于UDP远程发现与配置
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2026年1月7日
 */
final class Backdoor implements ChainGenericsHandler<UDPSlave, Parameters> {

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
		server = new UDPServer(this, host, port > 0 ? port : 1030);
		server.receive();
	}

	public void close() {
		if (server != null) {
			server.close();
			server = null;
		}
	}

	@Override
	public void connected(UDPSlave slave) throws Exception {
	}

	@Override
	public Parameters decode(UDPSlave slave, DataBuffer buffer) throws Exception {
		final byte value = buffer.backByte();
		if (value == 0x05) {
			final Parameters parameters = new Parameters();
			KeyValueCoder.decode(parameters, buffer);
			return parameters;
		} else {
			buffer.clear();
			return null;
		}
	}

	@Override
	public void received(UDPSlave slave, Parameters message) throws Exception {
		if (message != null) {
			if (message.isEmpty()) {
			} else {
				setting.update(message);
				Application.reset();
				setting.save();
			}
			setting.extract(message);
			slave.send(message);
		}
	}

	@Override
	public DataBuffer encode(UDPSlave slave, Parameters message) throws Exception {
		final DataBuffer buffer = DataBuffer.instance();
		KeyValueCoder.encode(message, buffer);
		buffer.writeByte(0x05);
		return buffer;
	}

	@Override
	public void sent(UDPSlave slave, Parameters message) throws Exception {
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

	// 避免泛型警告
	static class Parameters extends HashMap<String, String> {
		private static final long serialVersionUID = 1L;
	}
}