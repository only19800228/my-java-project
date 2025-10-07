package com.Quantitative.all.Test.DataFeed;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * 策略测试综合运行器 - 一键运行所有策略测试
 */
public class StrategyTestRunner {

	public static void main(String[] args) {
		System.out.println("=== 开始运行所有单指标策略测试 ===\n");

		// 所有策略测试类
		Class<?>[] testClasses = { ADXStrategyTest.class, AroonStrategyTest.class, ATRStrategyTest.class,
				BollingerBandsStrategyTest.class, CCIStrategyTest.class, EnhancedRSIStrategyTest.class,
				KDJStrategyTest.class, KeltnerChannelStrategyTest.class, MACDStrategyTest.class,
				MovingAverageStrategyTest.class, OBVStrategyTest.class, UltimateOscillatorStrategyTest.class };

		int totalTests = 0;
		int failedTests = 0;

		for (Class<?> testClass : testClasses) {
			System.out.println("运行测试: " + testClass.getSimpleName());
			Result result = JUnitCore.runClasses(testClass);

			totalTests += result.getRunCount();
			failedTests += result.getFailureCount();

			if (result.wasSuccessful()) {
				System.out.println("✓ " + testClass.getSimpleName() + " 测试通过\n");
			} else {
				System.out.println("✗ " + testClass.getSimpleName() + " 测试失败");
				for (Failure failure : result.getFailures()) {
					System.out.println("  失败: " + failure.getMessage());
				}
				System.out.println();
			}
		}

		// 输出总结
		System.out.println("=== 测试总结 ===");
		System.out.printf("总测试数: %d%n", totalTests);
		System.out.printf("通过: %d%n", totalTests - failedTests);
		System.out.printf("失败: %d%n", failedTests);
		System.out.printf("成功率: %.1f%%%n", (double) (totalTests - failedTests) / totalTests * 100);

		if (failedTests == 0) {
			System.out.println("\n🎉 所有策略测试通过！");
		} else {
			System.out.println("\n❌ 有测试失败，请检查相关策略实现");
		}
	}
}