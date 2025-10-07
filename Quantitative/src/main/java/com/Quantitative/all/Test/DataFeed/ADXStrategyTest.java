package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.ADXStrategy;

/**
 * ADX平均趋向指数策略测试
 */
public class ADXStrategyTest extends BaseStrategyTest {

	@Test
	public void testADXStrategyBasic() {
		System.out.println("=== ADX策略基础测试 ===");

		ADXStrategy strategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		// ADX特定验证
		result.printSummary();
		System.out.println("ADX策略测试完成");
	}

	@Test
	public void testADXStrategyWithDifferentPeriods() {
		System.out.println("=== ADX策略不同参数测试 ===");

		// 测试不同周期参数
		int[] periods = { 7, 14, 21 };
		for (int period : periods) {
			ADXStrategy strategy = new ADXStrategy(period, 25.0, 20.0, 20.0);
			strategy.setDebugMode(false);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("ADX(%d) 测试完成: 收益率=%.2f%%, 交易次数=%d%n", period, result.getTotalReturn(),
					result.getTotalTrades());
		}
	}

	@Test
	public void testADXStrategyThresholds() {
		System.out.println("=== ADX策略阈值测试 ===");

		// 测试不同阈值
		double[] thresholds = { 20.0, 25.0, 30.0 };
		for (double threshold : thresholds) {
			ADXStrategy strategy = new ADXStrategy(14, threshold, threshold - 5, threshold - 5);
			strategy.setDebugMode(false);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("ADX阈值%.1f测试完成: 收益率=%.2f%%, 胜率=%.1f%%%n", threshold, result.getTotalReturn(),
					result.getWinRate());
		}
	}
}