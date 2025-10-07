package com.Quantitative.all.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;

/**
 * 策略测试基类 - 提供通用的数据源设置
 */
public abstract class StrategyTestBase {

	protected DataFeed mockDataFeed;
	protected Portfolio mockPortfolio;
	protected List<BarEvent> testBars;

	@Before
	public void setUpBase() {
		// 创建模拟数据源
		mockDataFeed = mock(DataFeed.class);
		mockPortfolio = mock(Portfolio.class);

		// 生成测试数据
		testBars = generateTestBars(100);

		// 配置模拟数据源的行为
		when(mockDataFeed.getAllBars()).thenReturn(testBars);
		when(mockDataFeed.getNextBar()).thenAnswer(invocation -> {
			// 模拟数据流
			if (testBars.isEmpty())
				return null;
			return testBars.remove(0);
		});
		when(mockDataFeed.hasNextBar()).thenAnswer(invocation -> !testBars.isEmpty());
		when(mockDataFeed.isConnected()).thenReturn(true);
		when(mockDataFeed.getStatus()).thenReturn("CONNECTED");
	}

	protected List<BarEvent> generateTestBars(int count) {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 30);
		double price = 10.0;

		for (int i = 0; i < count; i++) {
			// 模拟真实的价格波动
			double change = (Math.random() - 0.5) * 0.8; // ±40%
			price = Math.max(0.1, price * (1 + change / 100));

			double open = price;
			double high = price * (1 + Math.random() * 0.03);
			double low = price * (1 - Math.random() * 0.03);
			double close = low + (high - low) * Math.random();
			long volume = (long) (1000000 + Math.random() * 2000000);

			BarEvent bar = new BarEvent(startTime.plusDays(i), "000001", open, high, low, close, volume);
			bars.add(bar);
		}

		return bars;
	}

	protected void setupStrategyDependencies(com.Quantitative.strategy.base.BaseStrategy strategy) {
		strategy.setDataFeed(mockDataFeed);
		strategy.setPortfolio(mockPortfolio);
		strategy.setDebugMode(true);

		// 确保数据源已设置后再初始化
		strategy.initialize();
	}
}