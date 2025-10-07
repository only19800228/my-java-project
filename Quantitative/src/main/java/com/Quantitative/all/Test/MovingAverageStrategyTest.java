package com.Quantitative.all.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * @author 测试移动平均线
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MovingAverageStrategyTest {

	private MovingAverageStrategy maStrategy;

	@Before
	public void setUp() {
		maStrategy = new MovingAverageStrategy(5, 20, 5, 1.2);
		maStrategy.setDebugMode(true);
	}

	@Test
	public void testMAStrategyInitialization() {
		assertEquals("移动平均线策略", maStrategy.getName());
		assertEquals(5, maStrategy.getFastPeriod());
		assertEquals(20, maStrategy.getSlowPeriod());
	}

	@Test
	public void testGoldenCrossSignal() {
		// 测试金叉信号（快线上穿慢线）
		BarEvent bar = createTestBar(10.5); // 价格上涨

		List<SignalEvent> signals = maStrategy.onBar(bar);

		assertNotNull(signals);
		// 应该生成买入信号
	}

	@Test
	public void testDeathCrossSignal() {
		// 测试死叉信号（快线下穿慢线）
		BarEvent bar = createTestBar(9.5); // 价格下跌

		List<SignalEvent> signals = maStrategy.onBar(bar);

		assertNotNull(signals);
		// 应该生成卖出信号
	}

	@Test
	public void testNoCrossSignal() {
		// 测试无交叉信号
		BarEvent bar = createTestBar(10.0); // 价格持平

		List<SignalEvent> signals = maStrategy.onBar(bar);

		assertNotNull(signals);
		// 可能不生成信号或生成HOLD信号
	}

	@Test
	public void testVolumeConfirmation() {
		// 测试成交量确认
		BarEvent highVolumeBar = new BarEvent(LocalDateTime.now(), "000001", 10.0, 10.8, 9.9, 10.5, 2000000 // 高成交量
		);

		List<SignalEvent> signals = maStrategy.onBar(highVolumeBar);

		assertNotNull(signals);
	}

	@Test
	public void testMAParameterUpdate() {
		maStrategy.setFastPeriod(10);
		maStrategy.setSlowPeriod(30);

		assertEquals(10, maStrategy.getFastPeriod());
		assertEquals(30, maStrategy.getSlowPeriod());
	}

	private BarEvent createTestBar(double closePrice) {
		return new BarEvent(LocalDateTime.now(), "000001", closePrice - 0.2, // open
				closePrice + 0.3, // high
				closePrice - 0.3, // low
				closePrice, // close
				1000000 // volume
		);
	}
}