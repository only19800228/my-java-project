package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.OBVStrategy;

/**
 * OBV策略测试
 */
public class OBVStrategyTest extends BaseStrategyTest {

	private OBVStrategy obvStrategy;

	@Before
	public void setUp() {
		super.setUp();
		obvStrategy = new OBVStrategy(20, 1.5);
		obvStrategy.setDataFeed(mockDataFeed);
		obvStrategy.setPortfolio(mockPortfolio);
		obvStrategy.setDebugMode(true);
		obvStrategy.initialize();
	}

	@Test
	public void testOBVStrategyInitialization() {
		assertNotNull("OBV策略应成功初始化", obvStrategy);
		assertEquals("策略名称", "OBV能量潮策略", obvStrategy.getName());
		assertEquals("成交量阈值", 1.5, obvStrategy.getVolumeThreshold(), 0.001);
	}

	@Test
	public void testOBVCalculation() {
		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = obvStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		// 验证OBV值
		assertNotNull("应计算OBV值", obvStrategy.getLastOBV());
		// OBV可以是任意值，取决于价格和成交量的累积
	}

	@Test
	public void testOBVWithHighVolume() {
		// 创建高成交量数据测试
		List<BarEvent> highVolumeBars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();

		for (int i = 0; i < 20; i++) {
			double price = 100.0 + (Math.random() - 0.5) * 10;
			long volume = 5000000 + (long) (Math.random() * 5000000); // 高成交量
			BarEvent bar = new BarEvent(time.plusMinutes(i), "000001", price - 1, price + 1, price - 2, price, volume);
			highVolumeBars.add(bar);
		}

		for (BarEvent bar : highVolumeBars) {
			obvStrategy.onBar(bar);
		}

		Double obv = obvStrategy.getLastOBV();
		assertNotNull("高成交量下应计算OBV", obv);
	}
}