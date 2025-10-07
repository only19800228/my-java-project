package com.Quantitative.all.Test.indicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;

/**
 * 策略测试基类
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseStrategyTest {

	protected DataFeed mockDataFeed;
	protected Portfolio mockPortfolio;
	protected List<BarEvent> testBars;

	@Before
	public void setUp() {
		mockDataFeed = mock(DataFeed.class);
		mockPortfolio = mock(Portfolio.class);
		testBars = createTestBars();

		when(mockDataFeed.getAllBars()).thenReturn(testBars);
	}

	protected List<BarEvent> createTestBars() {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 9, 30);

		// 创建测试数据 - 模拟价格波动
		double basePrice = 100.0;
		for (int i = 0; i < 100; i++) {
			double open = basePrice + Math.sin(i * 0.1) * 5;
			double high = open + Math.random() * 3;
			double low = open - Math.random() * 3;
			double close = open + (Math.random() - 0.5) * 2;
			long volume = 1000000 + (long) (Math.random() * 500000);

			BarEvent bar = new BarEvent(startTime.plusDays(i), "600089", open, high, low, close, volume);
			bars.add(bar);
		}

		return bars;
	}

	protected BarEvent createTestBarEvent(double close) {
		return new BarEvent(LocalDateTime.now(), "600089", close - 1, close + 1, close - 2, close, 1000000);
	}

	protected void verifySignal(SignalEvent signal, String expectedType, double minStrength) {
		assertNotNull("信号不应为空", signal);
		assertEquals("信号类型", expectedType, signal.getSignalType());
		assertTrue("信号强度应大于" + minStrength, signal.getStrength() >= minStrength);
		assertEquals("标的代码", "600089", signal.getSymbol());
	}
}