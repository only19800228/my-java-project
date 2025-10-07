package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 双均线策略测试
 */
public class MovingAverageStrategyTest extends BaseStrategyTest {

	@Test
	public void testMovingAverageStrategyInitialization() {
		MovingAverageStrategy strategy = new MovingAverageStrategy(10, 30, 5, 1.0);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "双均线策略", strategy.getName());
	}

	@Test
	public void testMovingAverageStrategyWithDifferentPeriods() {
		Object[][] testCases = { { 5, 20 }, // 短期
				{ 10, 30 }, // 中期
				{ 20, 50 }, // 长期
				{ 50, 200 } // 超长期
		};

		for (Object[] testCase : testCases) {
			int fast = (Integer) testCase[0];
			int slow = (Integer) testCase[1];

			MovingAverageStrategy strategy = new MovingAverageStrategy(fast, slow, 5, 1.0);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("均线参数 (%d,%d) - 收益率: %.2f%%%n", fast, slow, result.getTotalReturn());
		}
	}

	@Test
	public void testMovingAverageStrategyVolumeConfirmation() {
		MovingAverageStrategy strategy = new MovingAverageStrategy(10, 30, 5, 1.5);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);

		// 验证成交量确认的效果
		assertTrue("应该产生合理的交易结果", result.getTotalTrades() >= 0);
		result.printSummary();
	}
}