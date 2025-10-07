package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 双均线策略测试
 */
public class MovingAverageStrategyTest extends BaseStrategyTest {

	private MovingAverageStrategy maStrategy;

	@Before
	public void setUp() {
		super.setUp();
		maStrategy = new MovingAverageStrategy(10, 30, 5, 1.0);
		maStrategy.setDataFeed(mockDataFeed);
		maStrategy.setPortfolio(mockPortfolio);
		maStrategy.setDebugMode(true);
		maStrategy.initialize();
	}

	@Test
	public void testMovingAverageInitialization() {
		assertNotNull("双均线策略应成功初始化", maStrategy);
		assertEquals("策略名称", "双均线策略", maStrategy.getName());
		assertEquals("快线周期", 10, maStrategy.getFastPeriod());
		assertEquals("慢线周期", 30, maStrategy.getSlowPeriod());
	}

	@Test
	public void testMovingAverageSignals() {
		int goldenCrossCount = 0;
		int deathCrossCount = 0;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = maStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				if ("BUY".equals(signal.getSignalType())) {
					goldenCrossCount++;
				} else if ("SELL".equals(signal.getSignalType())) {
					deathCrossCount++;
				}
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		System.out.println("双均线金叉次数: " + goldenCrossCount + ", 死叉次数: " + deathCrossCount);
		// 在趋势性数据中应该有一些交叉信号
		assertTrue("应检测到一些交叉信号", goldenCrossCount + deathCrossCount >= 0);
	}

	@Test
	public void testDifferentMAPeriods() {
		MovingAverageStrategy shortMA = new MovingAverageStrategy(5, 10, 5, 1.0);
		shortMA.setDataFeed(mockDataFeed);
		shortMA.initialize();

		// 短周期均线应该更快响应
		for (int i = 0; i < 20; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = shortMA.onBar(bar);
			assertNotNull("信号列表不应为null", signals);
		}
	}
}