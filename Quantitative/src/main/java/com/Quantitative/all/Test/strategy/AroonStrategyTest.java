package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.AroonStrategy;

/**
 * Aroon指标策略测试
 */
public class AroonStrategyTest extends BaseStrategyTest {

	@Test
	public void testAroonStrategyInitialization() {
		AroonStrategy strategy = new AroonStrategy(14, 70.0);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "Aroon指标策略", strategy.getName());
	}

	@Test
	public void testAroonStrategyWithDefaultParameters() {
		AroonStrategy strategy = new AroonStrategy();
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("Aroon策略测试完成 - 总收益率: " + result.getTotalReturn() + "%");
	}

	@Test
	public void testAroonStrategyPerformance() {
		AroonStrategy strategy = new AroonStrategy(25, 80.0);
		strategy.setDebugMode(false);

		BacktestResult result = runStrategyTest(strategy);

		// 验证性能指标
		assertTrue("夏普比率应该合理", result.getSharpeRatio() > -10);
		assertTrue("最大回撤应该合理", result.getMaxDrawdown() >= -100);

		result.printSummary();
	}
}