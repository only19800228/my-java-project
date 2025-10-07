package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.MACDStrategy;

/**
 * MACD策略测试
 */
public class MACDStrategyTest extends BaseStrategyTest {

	@Test
	public void testMACDStrategyInitialization() {
		MACDStrategy strategy = new MACDStrategy(12, 26, 9, false);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "MACD策略", strategy.getName());
	}

	@Test
	public void testMACDStrategyWithZeroCross() {
		MACDStrategy strategy = new MACDStrategy(12, 26, 9, true);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("MACD零轴交叉策略测试完成");
	}

	@Test
	public void testMACDStrategyDifferentPeriods() {
		Object[][] testCases = { { 8, 17, 9 }, // 快速参数
				{ 12, 26, 9 }, // 标准参数
				{ 21, 52, 18 } // 慢速参数
		};

		for (Object[] testCase : testCases) {
			int fast = (Integer) testCase[0];
			int slow = (Integer) testCase[1];
			int signal = (Integer) testCase[2];

			MACDStrategy strategy = new MACDStrategy(fast, slow, signal, false);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("MACD参数 (%d,%d,%d) - 交易次数: %d%n", fast, slow, signal, result.getTotalTrades());
		}
	}
}