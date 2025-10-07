package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.UltimateOscillatorStrategy;

/**
 * UO终极振荡器策略测试
 */
public class UltimateOscillatorStrategyTest extends BaseStrategyTest {

	@Test
	public void testUltimateOscillatorStrategyBasic() {
		System.out.println("=== UO终极振荡器策略基础测试 ===");

		UltimateOscillatorStrategy strategy = new UltimateOscillatorStrategy(7, 14, 28, 70.0, 30.0);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("UO策略测试完成");
	}

	@Test
	public void testUltimateOscillatorStrategyPeriods() {
		System.out.println("=== UO策略周期组合测试 ===");

		// 测试不同的周期组合
		int[][] periodCombinations = { { 5, 10, 20 }, // 短周期组合
				{ 7, 14, 28 }, // 标准组合
				{ 10, 20, 40 } // 长周期组合
		};

		for (int[] periods : periodCombinations) {
			UltimateOscillatorStrategy strategy = new UltimateOscillatorStrategy(periods[0], periods[1], periods[2],
					70.0, 30.0);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("UO(%d,%d,%d) 测试: 收益率=%.2f%%, 交易次数=%d%n", periods[0], periods[1], periods[2],
					result.getTotalReturn(), result.getTotalTrades());
		}
	}
}