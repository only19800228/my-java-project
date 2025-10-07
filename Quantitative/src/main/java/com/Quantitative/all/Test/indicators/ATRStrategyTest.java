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
import com.Quantitative.strategy.indicators.ATRStrategy;

/**
 * ATR策略测试
 */
public class ATRStrategyTest extends BaseStrategyTest {

	private ATRStrategy atrStrategy;

	@Before
	public void setUp() {
		super.setUp();
		atrStrategy = new ATRStrategy(14, 2.0, 0.02);
		atrStrategy.setDataFeed(mockDataFeed);
		atrStrategy.setPortfolio(mockPortfolio);
		atrStrategy.setDebugMode(true);
		atrStrategy.initialize();
	}

	@Test
	public void testATRStrategyInitialization() {
		assertNotNull("ATR策略应成功初始化", atrStrategy);
		assertEquals("策略名称", "ATR波动率策略", atrStrategy.getName());
		assertEquals("ATR周期", 14, atrStrategy.getAtrPeriod());
		assertEquals("ATR乘数", 2.0, atrStrategy.getAtrMultiplier(), 0.001);
	}

	@Test
	public void testATRCalculation() {
		// 处理数据直到ATR被计算
		for (int i = 0; i < 20; i++) {
			atrStrategy.onBar(testBars.get(i));
		}

		Double atr = atrStrategy.getCurrentATR();
		assertNotNull("应计算ATR值", atr);
		assertTrue("ATR值应为正数", atr > 0);

		// 继续处理更多数据
		for (int i = 20; i < 40; i++) {
			List<SignalEvent> signals = atrStrategy.onBar(testBars.get(i));

			// 验证信号
			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}
	}

	@Test
	public void testATRWithHighVolatility() {
		// 创建高波动性数据
		List<BarEvent> highVolBars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();

		for (int i = 0; i < 30; i++) {
			double volatility = 10.0; // 高波动
			double base = 100.0 + i;
			double open = base;
			double high = base + Math.random() * volatility;
			double low = base - Math.random() * volatility;
			double close = base + (Math.random() - 0.5) * volatility;

			BarEvent bar = new BarEvent(time.plusMinutes(i), "000001", open, high, low, close, 1000000);
			highVolBars.add(bar);
		}

		// 使用高波动数据测试
		for (BarEvent bar : highVolBars) {
			atrStrategy.onBar(bar);
		}

		Double atr = atrStrategy.getCurrentATR();
		assertNotNull("高波动下应计算ATR", atr);
		assertTrue("高波动下ATR应较大", atr > 2.0);
	}
}