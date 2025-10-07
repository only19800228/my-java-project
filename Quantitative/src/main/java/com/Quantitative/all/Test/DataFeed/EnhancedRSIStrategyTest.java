package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 增强RSI策略测试
 */
public class EnhancedRSIStrategyTest extends BaseStrategyTest {

	@Test
	public void testEnhancedRSIStrategyBasic() {
		System.out.println("=== 增强RSI策略基础测试 ===");

		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("增强RSI策略测试完成");
	}

	@Test
	public void testRSIStrategyDifferentPeriods() {
		System.out.println("=== RSI策略周期测试 ===");

		int[] periods = { 7, 14, 21 };
		for (int period : periods) {
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(period, 70.0, 30.0, 0.02);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("RSI(%d) 测试完成: 收益率=%.2f%%, 夏普比率=%.2f%n", period, result.getTotalReturn(),
					result.getSharpeRatio());
		}
	}

	@Test
	public void testRSIStrategyThresholds() {
		System.out.println("=== RSI策略阈值测试 ===");

		// 测试不同的超买超卖阈值
		double[][] thresholds = { { 65.0, 35.0 }, { 70.0, 30.0 }, { 75.0, 25.0 } };

		for (double[] threshold : thresholds) {
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, threshold[0], threshold[1], 0.02);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("RSI(%.0f/%.0f) 测试: 收益率=%.2f%%, 胜率=%.1f%%%n", threshold[0], threshold[1],
					result.getTotalReturn(), result.getWinRate());
		}
	}

	@Test
	public void testRSIStrategyWithTrendFilter() {
		System.out.println("=== RSI策略趋势过滤测试 ===");

		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		strategy.setUseTrendFilter(true);
		strategy.setTrendThreshold(1.0);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.printf("RSI带趋势过滤测试: 收益率=%.2f%%, 交易次数=%d%n", result.getTotalReturn(), result.getTotalTrades());
	}
}