package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.KeltnerChannelStrategy;

/**
 * Keltner Channel策略测试
 */
public class KeltnerChannelStrategyTest extends BaseStrategyTest {

	@Test
	public void testKeltnerChannelStrategyInitialization() {
		KeltnerChannelStrategy strategy = new KeltnerChannelStrategy(20, 10, 2.0);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "Keltner Channel策略", strategy.getName());
	}

	@Test
	public void testKeltnerChannelWithDifferentATRMultipliers() {
		double[] multipliers = { 1.5, 2.0, 2.5, 3.0 };

		for (double multiplier : multipliers) {
			KeltnerChannelStrategy strategy = new KeltnerChannelStrategy(20, 10, multiplier);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("KC ATR乘数 %.1f - 夏普比率: %.2f%n", multiplier, result.getSharpeRatio());
		}
	}
}