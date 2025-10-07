package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.ADXStrategy;

/**
 * ADX平均趋向指数策略测试
 */
public class ADXStrategyTest extends BaseStrategyTest {

	@Test
	public void testADXStrategyInitialization() {
		ADXStrategy strategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "ADX趋势强度策略", strategy.getName());
	}

	@Test
	public void testADXStrategyWithDefaultParameters() {
		ADXStrategy strategy = new ADXStrategy();
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("ADX策略测试完成 - 总交易次数: " + result.getTotalTrades());
	}

	@Test
	public void testADXStrategyWithCustomParameters() {
		ADXStrategy strategy = new ADXStrategy(10, 30.0, 25.0, 25.0);
		strategy.setDebugMode(true);
		strategy.setParameter("useTrendFilter", true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
	}

	@Test
	public void testADXStrategyWithDifferentPeriods() {
		// 测试不同周期参数
		int[] periods = { 7, 14, 21 };

		for (int period : periods) {
			ADXStrategy strategy = new ADXStrategy(period, 25.0, 20.0, 20.0);
			BacktestResult result = runStrategyTest(strategy);

			assertTrue("周期 " + period + " 应该产生合理结果", result.getTotalTrades() >= 0);
		}
	}
}