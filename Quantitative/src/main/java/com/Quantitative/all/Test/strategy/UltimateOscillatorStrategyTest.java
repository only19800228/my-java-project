package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.UltimateOscillatorStrategy;

/**
 * UO终极振荡器策略测试
 */
public class UltimateOscillatorStrategyTest extends BaseStrategyTest {

	@Test
	public void testUltimateOscillatorStrategyInitialization() {
		UltimateOscillatorStrategy strategy = new UltimateOscillatorStrategy(7, 14, 28, 70.0, 30.0);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "UO终极振荡器策略", strategy.getName());
	}

	@Test
	public void testUltimateOscillatorStrategyWithDifferentPeriods() {
		Object[][] testCases = { { 5, 10, 20 }, // 短期
				{ 7, 14, 28 }, // 标准
				{ 10, 20, 40 } // 长期
		};

		for (Object[] testCase : testCases) {
			int shortPeriod = (Integer) testCase[0];
			int mediumPeriod = (Integer) testCase[1];
			int longPeriod = (Integer) testCase[2];

			UltimateOscillatorStrategy strategy = new UltimateOscillatorStrategy(shortPeriod, mediumPeriod, longPeriod,
					70.0, 30.0);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("UO周期 (%d,%d,%d) - 收益率: %.2f%%%n", shortPeriod, mediumPeriod, longPeriod,
					result.getTotalReturn());
		}
	}

	@Test
	public void testUltimateOscillatorStrategyDivergenceDetection() {
		UltimateOscillatorStrategy strategy = new UltimateOscillatorStrategy(7, 14, 28, 70.0, 30.0);
		strategy.setParameter("useDivergence", true);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		System.out.println("UO背离检测策略测试完成");
	}
}