package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 增强RSI策略测试
 */
public class EnhancedRSIStrategyTest extends BaseStrategyTest {

	@Test
	public void testEnhancedRSIStrategyInitialization() {
		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "增强RSI策略", strategy.getName());
	}

	@Test
	public void testEnhancedRSIStrategyWithDifferentPeriods() {
		int[] periods = { 7, 14, 21, 28 };

		for (int period : periods) {
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(period, 70.0, 30.0, 0.02);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("RSI周期 %d - 交易次数: %d, 胜率: %.1f%%%n", period, result.getTotalTrades(),
					result.getWinRate());
		}
	}

	@Test
	public void testEnhancedRSIStrategyWithTrendFilter() {
		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		strategy.setParameter("useTrendFilter", true);
		strategy.setParameter("trendThreshold", 1.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("带趋势过滤的RSI策略测试完成");
	}

	@Test
	public void testEnhancedRSIStrategyCachePerformance() {
		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		strategy.setParameter("useCache", true);

		long startTime = System.currentTimeMillis();
		BacktestResult result = runStrategyTest(strategy);
		long endTime = System.currentTimeMillis();

		System.out.printf("RSI策略执行时间: %d ms%n", (endTime - startTime));
		result.printSummary();
	}
}