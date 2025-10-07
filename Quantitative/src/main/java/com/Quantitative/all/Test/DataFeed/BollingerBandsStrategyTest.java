package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;

/**
 * 布林带策略测试
 */
public class BollingerBandsStrategyTest extends BaseStrategyTest {

	@Test
	public void testBollingerBandsStrategyBasic() {
		System.out.println("=== 布林带策略基础测试 ===");

		BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, 2.0, true, 0.1);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("布林带策略测试完成");
	}

	@Test
	public void testBollingerBandsStrategyParameters() {
		System.out.println("=== 布林带策略参数测试 ===");

		// 测试不同标准差倍数
		double[] stdDevs = { 1.5, 2.0, 2.5 };
		for (double stdDev : stdDevs) {
			BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, stdDev, true, 0.1);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("布林带(%.1fσ) 测试: 收益率=%.2f%%, 交易次数=%d%n", stdDev, result.getTotalReturn(),
					result.getTotalTrades());
		}
	}

	@Test
	public void testBollingerBandsWithAndWithoutSqueeze() {
		System.out.println("=== 布林带收缩检测测试 ===");

		// 测试启用和禁用收缩检测
		boolean[] squeezeOptions = { true, false };
		for (boolean useSqueeze : squeezeOptions) {
			BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, 2.0, useSqueeze, 0.1);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("布林带(收缩检测=%s) 测试: 收益率=%.2f%%, 胜率=%.1f%%%n", useSqueeze, result.getTotalReturn(),
					result.getWinRate());
		}
	}
}