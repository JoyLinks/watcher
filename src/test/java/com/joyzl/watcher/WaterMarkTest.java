package com.joyzl.watcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class WaterMarkTest {

	@Test
	void test() throws IOException {
		WaterMark waterMark = new WaterMark();
		long timestamp1 = waterMark.timestamp();
		waterMark.close();

		assertTrue(timestamp1 > 0);

		waterMark = new WaterMark();
		long timestamp2 = waterMark.timestamp();
		waterMark.close();

		assertEquals(timestamp1, timestamp2);
	}
}