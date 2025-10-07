package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.ATRStrategy;

/**
 * ATR平均真实波幅策略测试
 */
public class ATRStrategyTest extends BaseStrategyTest {

	@Test
	public void testATRStrategyBasic() {
		System.out.println("=== ATR策略基础测试 ===");

		ATRStrategy strategy = new ATRStrategy(14, 2.0, 0.02);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("ATR策略测试完成");
	}

	@Test
	public void testATRStrategyMultipliers() {
		System.out.println("=== ATR策略乘数测试 ===");

		double[] multipliers = { 1.5, 2.0, 2.5, 3.0 };
		for (double multiplier : multipliers) {
			ATRStrategy strategy = new ATRStrategy(14, multiplier, 0.02);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("ATR乘数%.1f测试: 收益率=%.2f%%, 交易次数=%d%n", multiplier, result.getTotalReturn(),
					result.getTotalTrades());
		}
	}

	@Test
	public void testATRStrategyPeriods() {
		System.out.println("=== ATR策略周期测试 ===");

		int[] periods = { 7, 14, 21 };
		for (int period : periods) {
			ATRStrategy strategy = new ATRStrategy(period, 2.0, 0.02);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("ATR(%d) 测试: 收益率=%.2f%%, 夏普比率=%.2f%n", period, result.getTotalReturn(),
					result.getSharpeRatio());
		}
	}
}