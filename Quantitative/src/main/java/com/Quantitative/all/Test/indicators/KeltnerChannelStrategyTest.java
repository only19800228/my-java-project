package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.KeltnerChannelStrategy;

/**
 * Keltner Channel策略测试
 */
public class KeltnerChannelStrategyTest extends BaseStrategyTest {

	private KeltnerChannelStrategy kcStrategy;

	@Before
	public void setUp() {
		super.setUp();
		kcStrategy = new KeltnerChannelStrategy(20, 10, 2.0);
		kcStrategy.setDataFeed(mockDataFeed);
		kcStrategy.setPortfolio(mockPortfolio);
		kcStrategy.setDebugMode(true);
		kcStrategy.initialize();
	}

	@Test
	public void testKeltnerChannelInitialization() {
		assertNotNull("Keltner Channel策略应成功初始化", kcStrategy);
		assertEquals("策略名称", "Keltner Channel策略", kcStrategy.getName());
		assertEquals("EMA周期", 20, kcStrategy.getEmaPeriod());
		assertEquals("ATR周期", 10, kcStrategy.getAtrPeriod());
		assertEquals("ATR乘数", 2.0, kcStrategy.getAtrMultiplier(), 0.001);
	}

	@Test
	public void testKeltnerChannelCalculation() {
		// 处理足够的数据来计算通道
		for (int i = 0; i < 30; i++) {
			kcStrategy.onBar(testBars.get(i));
		}

		assertNotNull("应计算上轨", kcStrategy.getUpperBand());
		assertNotNull("应计算中轨", kcStrategy.getMiddleBand());
		assertNotNull("应计算下轨", kcStrategy.getLowerBand());

		// 验证通道关系
		assertTrue("上轨应大于中轨", kcStrategy.getUpperBand() > kcStrategy.getMiddleBand());
		assertTrue("下轨应小于中轨", kcStrategy.getLowerBand() < kcStrategy.getMiddleBand());
	}

	@Test
	public void testChannelBreakoutSignals() {
		int breakoutSignals = 0;

		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = kcStrategy.onBar(bar);
			breakoutSignals += signals.size();

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		System.out.println("Keltner Channel突破信号数量: " + breakoutSignals);
	}
}