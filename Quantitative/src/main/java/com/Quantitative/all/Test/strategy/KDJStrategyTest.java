package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.KDJStrategy;

/**
 * KDJ随机指标策略测试
 */
public class KDJStrategyTest extends BaseStrategyTest {

	@Test
	public void testKDJStrategyInitialization() {
		KDJStrategy strategy = new KDJStrategy(9, 3, 80, 20);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "KDJ随机指标策略", strategy.getName());
	}

	@Test
	public void testKDJStrategyParameterCombinations() {
		// 测试不同的参数组合
		Object[][] testCases = { { 9, 3, 80, 20 }, // 标准参数
				{ 14, 5, 70, 30 }, // 中期参数
				{ 5, 2, 90, 10 } // 短期参数
		};

		for (Object[] testCase : testCases) {
			int kPeriod = (Integer) testCase[0];
			int dPeriod = (Integer) testCase[1];
			int overbought = (Integer) testCase[2];
			int oversold = (Integer) testCase[3];

			KDJStrategy strategy = new KDJStrategy(kPeriod, dPeriod, overbought, oversold);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("KDJ参数 (%d,%d,%d,%d) - 收益率: %.2f%%%n", kPeriod, dPeriod, overbought, oversold,
					result.getTotalReturn());
		}
	}
}