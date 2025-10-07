package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.KDJStrategy;

/**
 * KDJ策略测试
 */
public class KDJStrategyTest extends BaseStrategyTest {

	private KDJStrategy kdjStrategy;

	@Before
	public void setUp() {
		super.setUp();
		kdjStrategy = new KDJStrategy(9, 3, 80, 20);
		kdjStrategy.setDataFeed(mockDataFeed);
		kdjStrategy.setPortfolio(mockPortfolio);
		kdjStrategy.setDebugMode(true);
		kdjStrategy.initialize();
	}

	@Test
	public void testKDJStrategyInitialization() {
		assertNotNull("KDJ策略应成功初始化", kdjStrategy);
		assertEquals("策略名称", "KDJ随机指标策略", kdjStrategy.getName());
		assertEquals("K周期", 9, kdjStrategy.getKPeriod());
		assertEquals("D周期", 3, kdjStrategy.getDPeriod());
	}

	@Test
	public void testKDJCalculation() {
		for (BarEvent bar : testBars) {
			List<SignalEvent> signals = kdjStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				verifySignal(signal, signal.getSignalType(), 0.1);
			}
		}

		// 验证KDJ值
		assertNotNull("应计算K值", kdjStrategy.getLastK());
		assertNotNull("应计算D值", kdjStrategy.getLastD());
		assertNotNull("应计算J值", kdjStrategy.getLastJ());

		// 验证值范围
		assertTrue("K值应在0-100之间", kdjStrategy.getLastK() >= 0 && kdjStrategy.getLastK() <= 100);
		assertTrue("D值应在0-100之间", kdjStrategy.getLastD() >= 0 && kdjStrategy.getLastD() <= 100);
	}

	@Test
	public void testKDJCrossSignals() {
		int goldenCrossCount = 0;
		int deathCrossCount = 0;

		for (int i = 0; i < 50; i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = kdjStrategy.onBar(bar);

			for (SignalEvent signal : signals) {
				if (signal.getComment() != null && signal.getComment().contains("金叉")) {
					goldenCrossCount++;
				} else if (signal.getComment() != null && signal.getComment().contains("死叉")) {
					deathCrossCount++;
				}
			}
		}

		System.out.println("KDJ金叉次数: " + goldenCrossCount + ", 死叉次数: " + deathCrossCount);
	}
}