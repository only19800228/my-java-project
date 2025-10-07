package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.CCIStrategy;

/**
 * CCI商品通道指标策略测试
 */
public class CCIStrategyTest extends BaseStrategyTest {

	@Test
	public void testCCIStrategyBasic() {
		System.out.println("=== CCI策略基础测试 ===");

		CCIStrategy strategy = new CCIStrategy(14, 100.0, -100.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("CCI策略测试完成");
	}

	@Test
	public void testCCIStrategyThresholds() {
		System.out.println("=== CCI策略阈值测试 ===");

		// 测试不同的超买超卖阈值
		double[][] thresholds = { { 80.0, -80.0 }, { 100.0, -100.0 }, { 120.0, -120.0 } };

		for (double[] threshold : thresholds) {
			CCIStrategy strategy = new CCIStrategy(14, threshold[0], threshold[1]);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("CCI(%.0f/%.0f) 测试: 收益率=%.2f%%, 交易频率=%.1f%n", threshold[0], threshold[1],
					result.getTotalReturn(), result.getTotalTrades() / 120.0); // 近似半年交易频率
		}
	}
}