package com.Quantitative.all.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.all.Test.MockDataFeed;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.data.DataInfo;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 使用真实模拟数据源的RSI策略测试
 */
public class EnhancedRSIStrategyWithRealDataTest {

	private EnhancedRSIStrategy rsiStrategy;
	private DataFeed dataFeed;

	@Before
	public void setUp() {
		// 创建真实的模拟数据源（非Mockito mock）
		dataFeed = new MockDataFeed();

		// 生成更真实的测试数据
		List<BarEvent> testBars = generateRealisticTestData();
		((MockDataFeed) dataFeed).setTestData(testBars);

		// 初始化数据源
		dataFeed.initialize();

		// 创建策略
		rsiStrategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		rsiStrategy.setDataFeed(dataFeed);
		rsiStrategy.setDebugMode(true);
		rsiStrategy.initialize();
	}

	@Test
	public void testWithRealDataFeed() {
		assertNotNull("数据源应该成功设置", rsiStrategy.getDataFeed());
		assertTrue("策略应该已初始化", rsiStrategy.isInitialized());

		// 处理所有数据
		int totalSignals = 0;
		int processedBars = 0;

		while (dataFeed.hasNextBar()) {
			BarEvent bar = dataFeed.getNextBar();
			List<SignalEvent> signals = rsiStrategy.onBar(bar);
			totalSignals += signals.size();
			processedBars++;

			// 验证信号质量
			for (SignalEvent signal : signals) {
				assertValidSignal(signal, bar);
			}
		}

		System.out.printf("处理了 %d 个Bar，生成 %d 个信号%n", processedBars, totalSignals);
		assertTrue("应该处理了所有Bar", processedBars > 0);
	}

	@Test
	public void testDataFeedIntegration() {
		// 测试数据源与策略的集成
		assertEquals("INITIALIZED", dataFeed.getStatus());
		assertEquals("INITIALIZED", rsiStrategy.getStatus());

		assertTrue("数据源应该已连接", dataFeed.isConnected());
		assertFalse("可用标的列表不应为空", dataFeed.getAvailableSymbols().isEmpty());

		DataInfo dataInfo = dataFeed.getDataInfo();
		assertNotNull("数据信息不应为null", dataInfo);
		assertTrue("数据条数应该大于0", dataInfo.getBarCount() > 0);
	}

	private List<BarEvent> generateRealisticTestData() {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 30);
		double price = 50.0; // 起始价格

		// 生成200个交易日的模拟数据
		for (int day = 0; day < 200; day++) {
			LocalDateTime barTime = startTime.plusDays(day);

			// 模拟日内的4个价格：开、高、低、收
			double open = price;
			// 日内波动
			double dailyChange = (Math.random() - 0.5) * 0.04; // ±2%
			double high = open * (1 + Math.abs(dailyChange) * 0.8);
			double low = open * (1 - Math.abs(dailyChange) * 0.8);
			double close = open * (1 + dailyChange);

			// 更新下一个开盘价（接近前一个收盘价）
			price = close * (1 + (Math.random() - 0.5) * 0.01);

			long volume = (long) (10000000 + Math.random() * 20000000); // 1000万-3000万

			BarEvent bar = new BarEvent(barTime, "000001", open, high, low, close, volume);
			bars.add(bar);
		}

		return bars;
	}

	private void assertValidSignal(SignalEvent signal, BarEvent bar) {
		assertNotNull("信号不应为null", signal);
		assertEquals("信号标的应该匹配Bar的标的", bar.getSymbol(), signal.getSymbol());
		assertNotNull("信号时间不应为null", signal.getTimestamp());
		assertNotNull("信号类型不应为null", signal.getSignalType());
		assertTrue("信号强度应该在0-1之间", signal.getStrength() >= 0 && signal.getStrength() <= 1.0);
		assertNotNull("策略名称不应为null", signal.getStrategyName());
	}
}