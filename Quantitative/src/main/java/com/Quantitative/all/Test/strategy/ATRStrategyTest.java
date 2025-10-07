package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.ATRStrategy;

/**
 * ATR平均真实波幅策略测试
 */
public class ATRStrategyTest extends BaseStrategyTest {

	@Test
	public void testATRStrategyInitialization() {
		ATRStrategy strategy = new ATRStrategy(14, 2.0, 0.02);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "ATR波动率策略", strategy.getName());
	}

	@Test
	public void testATRStrategyWithDifferentMultipliers() {
		double[] multipliers = { 1.5, 2.0, 2.5, 3.0 };

		for (double multiplier : multipliers) {
			ATRStrategy strategy = new ATRStrategy(14, multiplier, 0.02);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("ATR乘数 %.1f - 交易次数: %d, 收益率: %.2f%%%n", multiplier, result.getTotalTrades(),
					result.getTotalReturn());
		}
	}

	@Test
	public void testATRStrategyRiskManagement() {
		ATRStrategy strategy = new ATRStrategy(14, 2.0, 0.015);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);

		// 验证风险管理效果
		assertTrue("最大回撤应该可控", result.getMaxDrawdown() > -50);
		assertTrue("胜率应该合理", result.getWinRate() >= 0);

		result.printTradeHistory();
	}
}