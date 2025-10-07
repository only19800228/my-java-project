package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.UltimateOscillatorStrategy;

/**
 * UO终极振荡器策略测试
 */
public class UltimateOscillatorStrategyTest extends BaseStrategyTest {

	private UltimateOscillatorStrategy uoStrategy;

	@Before
	public void setUp() {
		super.setUp();
		uoStrategy = new UltimateOscillatorStrategy(7, 14, 28, 70.0, 30.0);
		uoStrategy.setDataFeed(mockDataFeed);
		uoStrategy.setPortfolio(mockPortfolio);
		uoStrategy.setDebugMode(true);
		uoStrategy.initialize();
	}

	@Test
	public void testUOStrategyInitialization() {
		assertNotNull("UO策略应成功初始化", uoStrategy);
		assertEquals("策略名称", "UO终极振荡器策略", uoStrategy.getName());
		assertEquals("短周期", 7, uoStrategy.getShortPeriod());
		assertEquals("中周期", 14, uoStrategy.getMediumPeriod());
		assertEquals("长周期", 28, uoStrategy.getLongPeriod());
	}

	@Test
	public void testUOCalculation() {
		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = uoStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		Double lastUO = uoStrategy.getLastUO();
		if (lastUO != null) {
			assertTrue("UO值应在0-100之间", lastUO >= 0 && lastUO <= 100);
		}
	}

	@Test
	public void testUOWithDifferentMarketConditions() {
		// 测试UO在不同市场条件下的表现
		UltimateOscillatorStrategy customUO = new UltimateOscillatorStrategy(5, 10, 20, 80.0, 20.0);
		customUO.setDataFeed(mockDataFeed);
		customUO.initialize();

		// 使用部分数据测试
		for (int i = 0; i < 35; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = customUO.onBar(bar);
			assertNotNull("信号列表不应为null", signals);
		}
	}
}