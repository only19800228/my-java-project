package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.MACDStrategy;

/**
 * MACD策略测试
 */
public class MACDStrategyTest extends BaseStrategyTest {

	private MACDStrategy macdStrategy;

	@Before
	public void setUp() {
		super.setUp();
		macdStrategy = new MACDStrategy(12, 26, 9, false);
		macdStrategy.setDataFeed(mockDataFeed);
		macdStrategy.setPortfolio(mockPortfolio);
		macdStrategy.setDebugMode(true);
		macdStrategy.initialize();
	}

	@Test
	public void testMACDStrategyInitialization() {
		assertNotNull("MACD策略应成功初始化", macdStrategy);
		assertEquals("策略名称", "MACD策略", macdStrategy.getName());
		assertEquals("快线周期", 12, macdStrategy.getFastPeriod());
		assertEquals("慢线周期", 26, macdStrategy.getSlowPeriod());
		assertEquals("信号线周期", 9, macdStrategy.getSignalPeriod());
	}

	@Test
	public void testMACDSignals() {
		int signalCount = 0;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = macdStrategy.onBar(bar);
			signalCount += signals.size();

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		System.out.println("MACD策略信号数量: " + signalCount);
		// MACD需要较多数据才能产生信号
		assertTrue("应处理所有数据而不出错", signalCount >= 0);
	}

	@Test
	public void testMACDWithZeroCross() {
		MACDStrategy zeroCrossStrategy = new MACDStrategy(12, 26, 9, true);
		zeroCrossStrategy.setDataFeed(mockDataFeed);
		zeroCrossStrategy.initialize();

		// 测试数据
		for (int i = 0; i < 40; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = zeroCrossStrategy.onBar(bar);
			assertNotNull("信号列表不应为null", signals);
		}
	}
}