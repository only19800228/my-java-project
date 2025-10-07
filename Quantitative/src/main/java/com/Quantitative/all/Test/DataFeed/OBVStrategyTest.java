package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.OBVStrategy;

/**
 * OBV能量潮策略测试
 */
public class OBVStrategyTest extends BaseStrategyTest {

	@Test
	public void testOBVStrategyBasic() {
		System.out.println("=== OBV策略基础测试 ===");

		OBVStrategy strategy = new OBVStrategy(20, 1.2);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("OBV策略测试完成");
	}

	@Test
	public void testOBVStrategyVolumeThresholds() {
		System.out.println("=== OBV策略成交量阈值测试 ===");

		double[] volumeThresholds = { 1.0, 1.2, 1.5, 2.0 };
		for (double threshold : volumeThresholds) {
			OBVStrategy strategy = new OBVStrategy(20, threshold);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("OBV成交量阈值%.1f测试: 收益率=%.2f%%, 交易质量=%.1f%n", threshold, result.getTotalReturn(),
					result.getWinRate());
		}
	}
}