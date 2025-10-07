package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.CCIStrategy;

/**
 * CCI策略测试
 */
public class CCIStrategyTest extends BaseStrategyTest {

	private CCIStrategy cciStrategy;

	@Before
	public void setUp() {
		super.setUp();
		cciStrategy = new CCIStrategy(20, 100.0, -100.0);
		cciStrategy.setDataFeed(mockDataFeed);
		cciStrategy.setPortfolio(mockPortfolio);
		cciStrategy.setDebugMode(true);
		cciStrategy.initialize();
	}

	@Test
	public void testCCIStrategyInitialization() {
		assertNotNull("CCI策略应成功初始化", cciStrategy);
		assertEquals("策略名称", "CCI商品通道策略", cciStrategy.getName());
		assertEquals("CCI周期", 20, cciStrategy.getCciPeriod());
		assertEquals("超买线", 100.0, cciStrategy.getOverbought(), 0.001);
		assertEquals("超卖线", -100.0, cciStrategy.getOversold(), 0.001);
	}

	@Test
	public void testCCICalculation() {
		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = cciStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		Double lastCCI = cciStrategy.getLastCCI();
		if (lastCCI != null) {
			// CCI理论上可以在很大范围内波动，但通常不会极端
			assertTrue("CCI值应在合理范围内", Math.abs(lastCCI) < 1000);
		}
	}

	@Test
	public void testCCIOverboughtOversold() {
		// 创建极端价格数据测试超买超卖
		List<BarEvent> extremeBars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();

		// 创建持续上涨数据（可能触发超买）
		for (int i = 0; i < 25; i++) {
			double price = 100.0 + i * 2; // 持续上涨
			BarEvent bar = new BarEvent(time.plusMinutes(i), "000001", price - 1, price + 1, price - 2, price, 1000000);
			extremeBars.add(bar);
		}

		for (BarEvent bar : extremeBars) {
			cciStrategy.onBar(bar);
		}

		Double cci = cciStrategy.getLastCCI();
		System.out.println("极端上涨后的CCI: " + cci);
	}
}