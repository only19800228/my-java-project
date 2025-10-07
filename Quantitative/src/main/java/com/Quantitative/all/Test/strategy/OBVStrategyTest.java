package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.OBVStrategy;

/**
 * OBV能量潮策略测试
 */
public class OBVStrategyTest extends BaseStrategyTest {

	@Test
	public void testOBVStrategyInitialization() {
		OBVStrategy strategy = new OBVStrategy(20, 1.2);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "OBV能量潮策略", strategy.getName());
	}

	@Test
	public void testOBVStrategyWithVolumeThresholds() {
		double[] thresholds = { 1.0, 1.2, 1.5, 2.0 };

		for (double threshold : thresholds) {
			OBVStrategy strategy = new OBVStrategy(20, threshold);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("OBV成交量阈值 %.1f - 交易次数: %d%n", threshold, result.getTotalTrades());
		}
	}
}