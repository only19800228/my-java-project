package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.MACDStrategy;

/**
 * MACD策略测试
 */
public class MACDStrategyTest extends BaseStrategyTest {

	@Test
	public void testMACDStrategyBasic() {
		System.out.println("=== MACD策略基础测试 ===");

		MACDStrategy strategy = new MACDStrategy(12, 26, 9, false);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("MACD策略测试完成");
	}

	@Test
	public void testMACDStrategyWithZeroCross() {
		System.out.println("=== MACD零轴穿越测试 ===");

		MACDStrategy strategy = new MACDStrategy(12, 26, 9, true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.printf("MACD零轴穿越测试: 收益率=%.2f%%, 交易次数=%d%n", result.getTotalReturn(), result.getTotalTrades());
	}

	@Test
	public void testMACDStrategyParameters() {
		System.out.println("=== MACD策略参数测试 ===");

		// 测试不同的快慢线参数
		int[][] parameters = { { 8, 17, 9 }, // 较短周期
				{ 12, 26, 9 }, // 标准周期
				{ 5, 35, 5 } // 非常规周期
		};

		for (int[] param : parameters) {
			MACDStrategy strategy = new MACDStrategy(param[0], param[1], param[2], false);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("MACD(%d,%d,%d) 测试: 收益率=%.2f%%, 夏普比率=%.2f%n", param[0], param[1], param[2],
					result.getTotalReturn(), result.getSharpeRatio());
		}
	}
}