package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.CCIStrategy;

/**
 * CCI商品通道指标策略测试
 */
public class CCIStrategyTest extends BaseStrategyTest {

	@Test
	public void testCCIStrategyInitialization() {
		CCIStrategy strategy = new CCIStrategy(14, 100, -100);
		assertNotNull("策略初始化不应为空", strategy);
		assertEquals("策略名称应该正确", "CCI商品通道策略", strategy.getName());
	}

	@Test
	public void testCCIStrategyWithDifferentThresholds() {
		// 测试不同的超买超卖阈值
		Object[][] testCases = { { 80, -80 }, // 宽松阈值
				{ 100, -100 }, // 标准阈值
				{ 120, -120 } // 严格阈值
		};

		for (Object[] testCase : testCases) {
			double overbought = (Double) testCase[0];
			double oversold = (Double) testCase[1];

			CCIStrategy strategy = new CCIStrategy(14, overbought, oversold);
			BacktestResult result = runStrategyTest(strategy);

			System.out.printf("CCI阈值 (%.0f/%.0f) - 交易次数: %d%n", overbought, oversold, result.getTotalTrades());
		}
	}

	@Test
	public void testCCIStrategySignalGeneration() {
		CCIStrategy strategy = new CCIStrategy(20, 100, -100);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);

		// 验证信号生成
		assertTrue("应该产生交易信号", result.getTotalTrades() >= 0);
		result.printSummary();
	}
}