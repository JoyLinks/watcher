/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月22日
 */
class ModelTest {

	@Test
	void testCB_CS20T() {
		Matcher m;

		final Pattern name = Pattern.compile("(?:^|[\\\\/])([\\x20-\\x7E]+)\\.jpg$");
		System.out.println(name);

		m = name.matcher("7949554364186635.jpg");
		assertTrue(m.find());

		m = name.matcher("2025\\xxx.jpg");
		assertTrue(m.find());
		m = name.matcher("2025/xxx.jpg");
		assertTrue(m.find());

		m = name.matcher("2025\\xxx.jpeg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx.jpeg");
		assertFalse(m.find());

		m = name.matcher("2025\\xxx 中国.jpg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx 中国.jpg");
		assertFalse(m.find());

		m = name.matcher("2025\\xxx - 副本(1).jpg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx - 副本(1).jpg");
		assertFalse(m.find());

		m = name.matcher("D:\\ScreenShot\\2026-4-6\\DHT32-2146816C\\155506-16054279-00-20260131-2256.jpg");
		assertTrue(m.find());

		m = name.matcher("D:\\ScreenShot\\2026-4-30\\1501311-DF727A01G\\YDA13115101GTD281750.jpg");
		assertTrue(m.find());

		final Pattern code = Pattern.compile(".*?([^\\\\/]+)\\.jpg$");
		System.out.println(code);

		m = code.matcher("7949554364186635.jpg");
		assertTrue(m.find());
		assertEquals(m.group(1), "7949554364186635");
		assertEquals(m.groupCount(), 1);

		m = code.matcher("2025\\xxx.jpg");
		assertTrue(m.find());
		assertEquals(m.group(1), "xxx");
		assertEquals(m.groupCount(), 1);

		m = code.matcher("2025/xxx.jpg");
		assertTrue(m.find());
		assertEquals(m.group(1), "xxx");
		assertEquals(m.groupCount(), 1);

		m = code.matcher("2025\\TEST\\xxx.jpg");
		assertTrue(m.find());
		assertEquals(m.group(1), "xxx");

		m = code.matcher("2025/TEST/xxx.jpg");
		assertTrue(m.find());
		assertEquals(m.group(1), "xxx");

		m = code.matcher("2025\\xxx.jpeg");
		assertFalse(m.find());

		m = code.matcher("D:\\ScreenShot\\2026-4-6\\DHT32-2146816C\\155506-16054279-00-20260131-2256.jpg");
		assertTrue(m.find());

		m = code.matcher("D:\\ScreenShot\\2026-4-30\\1501311-DF727A01G\\YDA13115101GTD281750.jpg");
		assertTrue(m.find());
	}

	@Test
	void testJX_B_F() {
		Matcher m;

		final Pattern name = Pattern.compile("(?:^|[\\\\/])(.+)\\.csv$");
		System.out.println(name);

		m = name.matcher("PEF112C02-1130：05-06-20-251119282 20251122 0307..csv");
		assertTrue(m.find());

		m = name.matcher("2025-10-22\\PEF１１２Ｃ０２－1130：05-06-20-251119282 20251122 0307..csv");
		assertTrue(m.find());
		m = name.matcher("2025-10-22/PEF１１２Ｃ０２－1130：05-06-20-251119282 20251122 0307..csv");
		assertTrue(m.find());

		m = name.matcher("2025\\xxx.jpeg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx.jpeg");
		assertFalse(m.find());

		m = name.matcher("2025\\xxx 中国.jpg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx 中国.jpg");
		assertFalse(m.find());

		m = name.matcher("2025\\xxx - 副本(1).jpg");
		assertFalse(m.find());
		m = name.matcher("2025/xxx - 副本(1).jpg");
		assertFalse(m.find());

		final Pattern code = Pattern.compile("：\\d+-\\d+-\\d+-([\\x20-\\x7E]+?)\\.");
		System.out.println(code);

		m = code.matcher("PEF112C02-1130：05-06-20-251119282 20251122 0307..csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "251119282 20251122 0307");

		m = code.matcher("2025\\PEF112C02-1130：05-06-20-251119282 20251122 0307.csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "251119282 20251122 0307");

		m = code.matcher("2025/PEF１１２Ｃ０２-1130：05-06-20-251119282 20251122 0307.csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "251119282 20251122 0307");

		m = code.matcher("2025\\TEST\\PEF１１２Ｃ０２-1130：05-06-20-251119282 20251122 0307.csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "251119282 20251122 0307");

		m = code.matcher("2025/TEST/PEF112C02-1130：05-06-20-251119282 20251122 0307.csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "251119282 20251122 0307");

		m = code.matcher("2025\\xxx.csv");
		assertFalse(m.find());

		m = code.matcher("2025-12-22\\P000632402070：09-40-57-S03447000632512213456.csv");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "S03447000632512213456");

		m = code.matcher("2025-12-22\\P515-D：14-57-48-.csv");
		assertFalse(m.find());
	}

	@Test
	void testDIR_N_N() {
		// ([ -~&&[^\\\\/]]+-[ -~&&[^\\\\/]]+)

		final Pattern name = Pattern.compile("([ -~&&[^\\\\/]]+-[ -~&&[^\\\\/]]+)");
		System.out.println(name);

		Matcher m;
		m = name.matcher("2026年6月13日\\YDA11117523DTF080062-YDA11307523ETF100376");
		assertTrue(m.find());

		m = name.matcher("2026年6月13日\\YDA11307523ETF100376");
		assertFalse(m.find());

		m = name.matcher("2026年6月13日");
		assertFalse(m.find());

		m = name.matcher("123456789-098765432\\1 (1).jpg");
		assertTrue(m.find());

		m = name.matcher("214601667-mcccmaw260614y0831+260601245 20260604 0061\\1.jpg");
		assertTrue(m.find());

		final Pattern code = Pattern.compile("([ -~&&[^\\\\/]]+-[ -~&&[^\\\\/]]+)");
		System.out.println(code);

		m = code.matcher("YDA11117523DTF080062-YDA11307523ETF100376");
		assertTrue(m.find());
		assertEquals(m.groupCount(), 1);
		assertEquals(m.group(1), "YDA11117523DTF080062-YDA11307523ETF100376");
	}
}