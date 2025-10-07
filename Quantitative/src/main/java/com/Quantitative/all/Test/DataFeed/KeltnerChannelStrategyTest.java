package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.KeltnerChannelStrategy;

/**
 * Keltner Channel策略测试
 */
public class KeltnerChannelStrategyTest extends BaseStrategyTest {

	@Test
	public void testKeltnerChannelStrategyBasic() {
		System.out.println("=== Keltner Channel策略基础测试 ===");

		KeltnerChannelStrategy strategy = new KeltnerChannelStrategy(20, 10, 2.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("Keltner Channel策略测试完成");
	}

	@Test
	public void testKeltnerChannelStrategyMultipliers() {
		System.out.println("=== Keltner Channel乘数测试 ===");

		double[] multipliers = { 1.5, 2.0, 2.5 };
		for (double multiplier : multipliers) {
			KeltnerChannelStrategy strategy = new KeltnerChannelStrategy(20, 10, multiplier);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("KC乘数%.1f测试: 收益率=%.2f%%, 最大回撤=%.2f%%%n", multiplier, result.getTotalReturn(),
					result.getMaxDrawdown());
		}
	}
}