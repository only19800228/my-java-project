package com.Quantitative.all.Test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * 测试运行器
 */
public class StrategyTestRunner {

	public static void main(String[] args) {
		System.out.println("开始运行策略测试...");

		// 运行所有测试类
		Class<?>[] testClasses = { EnhancedRSIStrategyTest.class, EnhancedRSIStrategyWithRealDataTest.class,
				MovingAverageStrategyTest.class };

		for (Class<?> testClass : testClasses) {
			System.out.println("\n运行测试: " + testClass.getSimpleName());
			Result result = JUnitCore.runClasses(testClass);

			System.out.println("测试结果: " + (result.wasSuccessful() ? "通过" : "失败"));
			System.out.println("运行测试数: " + result.getRunCount());
			System.out.println("失败测试数: " + result.getFailureCount());

			for (Failure failure : result.getFailures()) {
				System.out.println("失败: " + failure.getDescription());
				System.out.println("异常: " + failure.getException());
			}
		}

		System.out.println("\n所有测试运行完成!");
	}
}