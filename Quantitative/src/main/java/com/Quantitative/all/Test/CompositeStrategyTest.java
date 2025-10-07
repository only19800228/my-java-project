package com.Quantitative.all.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.composite.MeanReversionComposite;
import com.Quantitative.strategy.composite.MomentumBreakoutComposite;
import com.Quantitative.strategy.composite.MultiTimeframeComposite;
import com.Quantitative.strategy.composite.TrendFollowingComposite;

@RunWith(MockitoJUnitRunner.class)
public class CompositeStrategyTest {

	private MeanReversionComposite meanReversionStrategy;
	private MomentumBreakoutComposite momentumStrategy;
	private MultiTimeframeComposite timeframeStrategy;
	private TrendFollowingComposite trendStrategy;

	private DataFeed mockDataFeed;
	private Portfolio mockPortfolio;

	@Before
	public void setUp() {
		mockDataFeed = mock(DataFeed.class);
		mockPortfolio = mock(Portfolio.class);

		meanReversionStrategy = new MeanReversionComposite();
		momentumStrategy = new MomentumBreakoutComposite();
		timeframeStrategy = new MultiTimeframeComposite();
		trendStrategy = new TrendFollowingComposite();

		// 设置依赖
		setupStrategy(meanReversionStrategy);
		setupStrategy(momentumStrategy);
		setupStrategy(timeframeStrategy);
		setupStrategy(trendStrategy);
	}

	private void setupStrategy(BaseStrategy strategy) {
		strategy.setDataFeed(mockDataFeed);
		strategy.setPortfolio(mockPortfolio);
		strategy.setDebugMode(true);
		strategy.initialize();
	}

	@Test
	public void testMeanReversionStrategy() {
		BarEvent bar = createTestBar(9.5); // 低价，可能触发均值回归

		List<SignalEvent> signals = meanReversionStrategy.onBar(bar);

		assertNotNull(signals);
		assertEquals("均值回归组合策略", meanReversionStrategy.getName());
	}

	@Test
	public void testMomentumBreakoutStrategy() {
		BarEvent bar = createTestBar(11.0); // 高价突破

		List<SignalEvent> signals = momentumStrategy.onBar(bar);

		assertNotNull(signals);
		assertEquals("动量突破组合策略", momentumStrategy.getName());
	}

	@Test
	public void testMultiTimeframeStrategy() {
		BarEvent bar = createTestBar(10.2);

		List<SignalEvent> signals = timeframeStrategy.onBar(bar);

		assertNotNull(signals);
		assertEquals("多时间框架组合策略", timeframeStrategy.getName());
	}

	@Test
	public void testTrendFollowingStrategy() {
		BarEvent bar = createTestBar(10.8); // 上涨趋势

		List<SignalEvent> signals = trendStrategy.onBar(bar);

		assertNotNull(signals);
		assertEquals("趋势跟踪组合策略", trendStrategy.getName());
	}

	@Test
	public void testStrategyParameters() {
		// 测试参数设置
		meanReversionStrategy.setRsiLevels(75, 25);
		trendStrategy.setFastPeriod(10);
		trendStrategy.setSlowPeriod(30);

		assertNotNull(meanReversionStrategy.getParameters());
		assertNotNull(trendStrategy.getParameters());
	}

	@Test
	public void testStrategyStatus() {
		assertTrue(meanReversionStrategy.isInitialized());
		assertTrue(momentumStrategy.isInitialized());
		assertTrue(timeframeStrategy.isInitialized());
		assertTrue(trendStrategy.isInitialized());

		assertEquals("INITIALIZED", meanReversionStrategy.getStatus());
	}

	private BarEvent createTestBar(double closePrice) {
		return new BarEvent(LocalDateTime.now(), "000001", closePrice - 0.3, closePrice + 0.3, closePrice - 0.4,
				closePrice, 1500000);
	}
}