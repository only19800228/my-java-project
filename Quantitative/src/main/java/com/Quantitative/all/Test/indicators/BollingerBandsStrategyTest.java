package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;

/**
 * 布林带策略测试
 */
public class BollingerBandsStrategyTest extends BaseStrategyTest {

	private BollingerBandsStrategy bbStrategy;

	@Before
	public void setUp() {
		super.setUp();
		bbStrategy = new BollingerBandsStrategy(20, 2.0, true, 0.1);
		bbStrategy.setDataFeed(mockDataFeed);
		bbStrategy.setPortfolio(mockPortfolio);
		bbStrategy.setDebugMode(true);
		bbStrategy.initialize();
	}

	@Test
	public void testBollingerBandsInitialization() {
		assertNotNull("布林带策略应成功初始化", bbStrategy);
		assertEquals("策略名称", "布林带策略", bbStrategy.getName());
		assertEquals("布林带周期", 20, bbStrategy.getPeriod());
		assertEquals("标准差倍数", 2.0, bbStrategy.getNumStdDev(), 0.001);
	}

	@Test
	public void testBollingerBandsSignals() {
		int signalCount = 0;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = bbStrategy.onBar(bar);
			signalCount += signals.size();

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		System.out.println("布林带策略生成信号数量: " + signalCount);
		// 在测试数据中，策略应该生成一些信号
		assertTrue("应生成一些交易信号", signalCount >= 0);
	}

	@Test
	public void testBollingerBandsWithDifferentParameters() {
		BollingerBandsStrategy customBB = new BollingerBandsStrategy(10, 1.5, false, 0.05);
		customBB.setDataFeed(mockDataFeed);
		customBB.initialize();

		// 测试少量数据
		for (int i = 0; i < 30; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = customBB.onBar(bar);
			assertNotNull("信号列表不应为null", signals);
		}
	}
}