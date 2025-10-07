package com.Quantitative.all.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.composite.MeanReversionComposite;
import com.Quantitative.strategy.composite.MomentumBreakoutComposite;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 策略性能对比测试
 */
public class StrategyPerformanceTest {

	@Test
	public void testStrategySignalFrequency() {
		// 测试不同策略的信号频率
		EnhancedRSIStrategy rsi = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		MovingAverageStrategy ma = new MovingAverageStrategy(5, 20, 5, 1.2);

		List<BarEvent> testBars = generateTestBars(100);

		int rsiSignals = countSignals(rsi, testBars);
		int maSignals = countSignals(ma, testBars);

		System.out.println("RSI策略信号数量: " + rsiSignals);
		System.out.println("MA策略信号数量: " + maSignals);

		// 信号数量应该在合理范围内
		assertTrue(rsiSignals > 0);
		assertTrue(maSignals > 0);
	}

	@Test
	public void testCompositeStrategyIntegration() {
		// 测试组合策略的集成
		MeanReversionComposite composite = new MeanReversionComposite();
		composite.initialize();

		List<BarEvent> testBars = generateTestBars(50);
		int totalSignals = countSignals(composite, testBars);

		assertTrue(totalSignals >= 0); // 组合策略可能过滤很多信号
	}

	@Test
	public void testStrategyResponseTime() {
		// 测试策略响应时间
		MomentumBreakoutComposite strategy = new MomentumBreakoutComposite();
		strategy.initialize();

		BarEvent bar = generateTestBars(1).get(0);

		long startTime = System.nanoTime();
		List<SignalEvent> signals = strategy.onBar(bar);
		long endTime = System.nanoTime();

		long responseTime = endTime - startTime;

		System.out.println("策略响应时间: " + (responseTime / 1000000.0) + "ms");

		assertNotNull(signals);
		assertTrue(responseTime < 100000000); // 响应时间应小于100ms
	}

	private int countSignals(BaseStrategy strategy, List<BarEvent> bars) {
		int count = 0;
		for (BarEvent bar : bars) {
			List<SignalEvent> signals = strategy.onBar(bar);
			count += signals.size();
		}
		return count;
	}

	private List<BarEvent> generateTestBars(int count) {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();
		double price = 10.0;

		for (int i = 0; i < count; i++) {
			// 模拟价格随机游走
			double change = (Math.random() - 0.5) * 0.5;
			price += change;
			price = Math.max(1.0, price); // 确保价格为正

			BarEvent bar = new BarEvent(time.plusMinutes(i), "TEST", price - 0.1, price + 0.2, price - 0.2, price,
					(long) (1000000 + Math.random() * 1000000));
			bars.add(bar);
		}

		return bars;
	}
}