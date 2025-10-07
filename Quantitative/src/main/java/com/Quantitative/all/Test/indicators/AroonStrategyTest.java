package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.AroonStrategy;

/**
 * Aroon策略测试
 */
public class AroonStrategyTest extends BaseStrategyTest {

	private AroonStrategy aroonStrategy;

	@Before
	public void setUp() {
		super.setUp();
		aroonStrategy = new AroonStrategy(14, 70.0);
		aroonStrategy.setDataFeed(mockDataFeed);
		aroonStrategy.setPortfolio(mockPortfolio);
		aroonStrategy.setDebugMode(true);
		aroonStrategy.initialize();
	}

	@Test
	public void testAroonStrategyInitialization() {
		assertNotNull("Aroon策略应成功初始化", aroonStrategy);
		assertEquals("策略名称", "Aroon指标策略", aroonStrategy.getName());
		assertEquals("Aroon周期", 14, aroonStrategy.getPeriod());
		assertEquals("强度阈值", 70.0, aroonStrategy.getStrengthThreshold(), 0.001);
	}

	@Test
	public void testAroonCalculation() {
		boolean hasSignals = false;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = aroonStrategy.onBar(bar);

			if (!signals.isEmpty()) {
				hasSignals = true;
				SignalEvent signal = signals.get(0);
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		// 验证Aroon值计算
		assertNotNull("应计算Aroon Up值", aroonStrategy.getLastAroonUp());
		assertNotNull("应计算Aroon Down值", aroonStrategy.getLastAroonDown());

		// 验证Aroon值范围
		assertTrue("Aroon Up应在0-100之间", aroonStrategy.getLastAroonUp() >= 0 && aroonStrategy.getLastAroonUp() <= 100);
		assertTrue("Aroon Down应在0-100之间",
				aroonStrategy.getLastAroonDown() >= 0 && aroonStrategy.getLastAroonDown() <= 100);
	}

	@Test
	public void testAroonOscillator() {
		// 处理足够的数据
		for (int i = 0; i < 50; i++) {
			aroonStrategy.onBar(testBars.get(i));
		}

		Double oscillator = aroonStrategy.getLastAroonOscillator();
		if (oscillator != null) {
			assertTrue("振荡器应在合理范围内", oscillator >= -100 && oscillator <= 100);
		}
	}
}