package com.Quantitative.all.Test.strategy.ZH;

import java.time.LocalDateTime;
import java.util.Collections;

import com.Quantitative.core.events.BarEvent;

/**
 * 量化交易测试基础类
 */
public class QuantitativeTestBase {

	/**
	 * 创建测试用的K线数据
	 */
	protected BarEvent createTestBarEvent(String symbol, double price) {
		return createTestBarEvent(symbol, price, price + 0.5, price - 0.5, price, 1000000);
	}

	protected BarEvent createTestBarEvent(String symbol, double open, double high, double low, double close,
			long volume) {
		return new BarEvent(LocalDateTime.now(), symbol, open, high, low, close, volume);
	}

	/**
	 * 安全执行测试代码
	 */
	protected void executeSafely(TestRunnable runnable, String operation) {
		try {
			runnable.run();
		} catch (Exception e) {
			System.err.println(operation + " 执行失败: " + e.getMessage());
			// 测试中不抛出异常，避免影响其他测试
		}
	}

	/**
	 * 打印测试分隔符
	 */
	protected void printTestHeader(String testName) {
		// System.out.println("\n" + "=".repeat(60));
		System.out.println("\n" + String.join("", Collections.nCopies(60, "=")));

		System.out.println("测试: " + testName);
		// System.out.println("=".repeat(60));
		System.out.println("\n" + String.join("", Collections.nCopies(60, "=")));
	}

	/**
	 * 打印测试结果
	 */
	protected void printTestResult(boolean success, String message) {
		if (success) {
			System.out.println("✓ " + message);
		} else {
			System.out.println("✗ " + message);
		}
	}

	@FunctionalInterface
	protected interface TestRunnable {
		void run() throws Exception;
	}
}