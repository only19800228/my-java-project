package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 增强RSI策略测试
 */
public class EnhancedRSIStrategyTest extends BaseStrategyTest {

	private EnhancedRSIStrategy rsiStrategy;

	@Before
	public void setUp() {
		super.setUp();
		rsiStrategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		rsiStrategy.setDataFeed(mockDataFeed);
		rsiStrategy.setPortfolio(mockPortfolio);
		rsiStrategy.setDebugMode(true);
		rsiStrategy.initialize();
	}

	@Test
	public void testRSIStrategyInitialization() {
		assertNotNull("RSI策略应成功初始化", rsiStrategy);
		assertEquals("策略名称", "增强RSI策略", rsiStrategy.getName());
		assertEquals("RSI周期", 14, rsiStrategy.getRsiPeriod());
		assertEquals("超买线", 70.0, rsiStrategy.getOverbought(), 0.001);
		assertEquals("超卖线", 30.0, rsiStrategy.getOversold(), 0.001);
	}

	@Test
	public void testRSICalculation() {
		int buySignals = 0;
		int sellSignals = 0;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = rsiStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				if ("BUY".equals(signal.getSignalType())) {
					buySignals++;
				} else if ("SELL".equals(signal.getSignalType())) {
					sellSignals++;
				}
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		System.out.println("RSI策略 - 买入信号: " + buySignals + ", 卖出信号: " + sellSignals);

		Double lastRSI = rsiStrategy.getLastRSI();
		assertNotNull("应计算RSI值", lastRSI);
		assertTrue("RSI值应在0-100之间", lastRSI >= 0 && lastRSI <= 100);
	}

	@Test
	public void testRSIWithExtremeConditions() {
		// 测试RSI在极端市场条件下的表现

		// 创建持续上涨数据（RSI应该进入超买区）
		List<BarEvent> risingBars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();

		for (int i = 0; i < 20; i++) {
			double price = 100.0 + i; // 持续上涨
			BarEvent bar = new BarEvent(time.plusDays(i), "000001", price - 0.5, price + 0.5, price - 1, price,
					1000000);
			risingBars.add(bar);
		}

		for (BarEvent bar : risingBars) {
			rsiStrategy.onBar(bar);
		}

		Double rsi = rsiStrategy.getLastRSI();
		System.out.println("持续上涨后的RSI: " + rsi);
		if (rsi != null) {
			assertTrue("持续上涨后RSI应较高", rsi > 50);
		}
	}

	@Test
	public void testRSIParameterValidation() {
		// 测试参数验证
		try {
			EnhancedRSIStrategy invalidStrategy = new EnhancedRSIStrategy(0, 70.0, 30.0, 0.02);
			fail("应拒绝无效的RSI周期");
		} catch (Exception e) {
			// 期望的异常
		}
	}
}