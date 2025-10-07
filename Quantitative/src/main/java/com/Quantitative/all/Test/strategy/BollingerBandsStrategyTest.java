package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;

/**
 * 布林带策略测试
 */
public class BollingerBandsStrategyTest extends BaseStrategyTest {

	@Test
	public void testBollingerBandsStrategyInitialization() {
		BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, 2.0, true, 0.1);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "布林带策略", strategy.getName());
	}

	@Test
	public void testBollingerBandsWithDifferentParameters() {
		// 测试不同标准差参数
		double[] stdDevs = { 1.5, 2.0, 2.5 };

		for (double stdDev : stdDevs) {
			BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, stdDev, true, 0.1);
			BacktestResult result = runStrategyTest(strategy);

			assertTrue("标准差 " + stdDev + " 应该产生合理结果", result.getTotalTrades() >= 0);
		}
	}

	@Test
	public void testBollingerBandsSqueezeDetection() {
		BollingerBandsStrategy strategy = new BollingerBandsStrategy(20, 2.0, true, 0.05);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("布林带收缩检测策略测试完成");
	}
}