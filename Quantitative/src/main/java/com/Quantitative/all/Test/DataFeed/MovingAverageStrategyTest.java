package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 双均线策略测试
 */
public class MovingAverageStrategyTest extends BaseStrategyTest {

	@Test
	public void testMovingAverageStrategyBasic() {
		System.out.println("=== 双均线策略基础测试 ===");

		MovingAverageStrategy strategy = new MovingAverageStrategy(10, 30, 5, 1.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("双均线策略测试完成");
	}

	@Test
	public void testMovingAverageStrategyCombinations() {
		System.out.println("=== 双均线策略组合测试 ===");

		// 测试不同的均线组合
		int[][] combinations = { { 5, 20 }, // 短周期
				{ 10, 30 }, // 中周期
				{ 20, 50 }, // 长周期
				{ 5, 60 } // 极长短组合
		};

		for (int[] combo : combinations) {
			MovingAverageStrategy strategy = new MovingAverageStrategy(combo[0], combo[1], 5, 1.0);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("MA(%d,%d) 测试: 收益率=%.2f%%, 交易次数=%d%n", combo[0], combo[1], result.getTotalReturn(),
					result.getTotalTrades());
		}
	}

	@Test
	public void testMovingAverageStrategyVolumeThreshold() {
		System.out.println("=== 双均线策略成交量阈值测试 ===");

		double[] volumeThresholds = { 0.8, 1.0, 1.2, 1.5 };
		for (double threshold : volumeThresholds) {
			MovingAverageStrategy strategy = new MovingAverageStrategy(10, 30, 5, threshold);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("MA成交量阈值%.1f测试: 收益率=%.2f%%, 胜率=%.1f%%%n", threshold, result.getTotalReturn(),
					result.getWinRate());
		}
	}
}