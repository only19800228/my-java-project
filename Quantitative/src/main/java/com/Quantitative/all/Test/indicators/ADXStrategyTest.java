package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.ADXStrategy;

/**
 * ADX策略测试
 */
public class ADXStrategyTest extends BaseStrategyTest {

	private ADXStrategy adxStrategy;

	@Before
	public void setUp() {
		super.setUp();
		adxStrategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
		adxStrategy.setDataFeed(mockDataFeed);
		adxStrategy.setPortfolio(mockPortfolio);
		adxStrategy.setDebugMode(true);
		adxStrategy.initialize();
	}

	@Test
	public void testADXStrategyInitialization() {
		assertNotNull("ADX策略应成功初始化", adxStrategy);
		assertEquals("策略名称", "ADX趋势强度策略", adxStrategy.getName());
		assertEquals("ADX周期", 14, adxStrategy.getAdxPeriod());
		assertEquals("超买阈值", 25.0, adxStrategy.getAdxThreshold(), 0.001);
	}

	@Test
	public void testADXCalculation() {
		// 处理所有测试数据
		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = adxStrategy.onBar(bar);

			// 验证信号格式
			if (!signals.isEmpty()) {
				for (SignalEvent signal : signals) {
					verifySignal(signal, signal.getSignalType(), 0.1);
				}
			}
		}

		// 验证ADX值计算
		assertNotNull("应计算ADX值", adxStrategy.getLastADX());
		assertTrue("ADX值应在合理范围内", adxStrategy.getLastADX() >= 0 && adxStrategy.getLastADX() <= 100);
	}

	@Test
	public void testADXWithDifferentParameters() {
		ADXStrategy customStrategy = new ADXStrategy(20, 30.0, 25.0, 25.0);
		customStrategy.setDataFeed(mockDataFeed);
		customStrategy.initialize();

		// 测试少量数据
		for (int i = 0; i < 50; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = customStrategy.onBar(bar);
			// 验证没有异常
			assertNotNull("信号列表不应为null", signals);
		}
	}

	@Test
	public void testADXReset() {
		// 先处理一些数据
		for (int i = 0; i < 30; i++) {
			adxStrategy.onBar(testBars.get(i));
		}

		// 重置策略
		adxStrategy.reset();

		// 验证重置后状态
		assertNull("重置后ADX值应为null", adxStrategy.getLastADX());
		assertEquals("策略状态", "RESET", adxStrategy.getStatus());
	}
}