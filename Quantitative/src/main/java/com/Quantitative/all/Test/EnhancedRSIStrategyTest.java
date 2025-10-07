package com.Quantitative.all.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

public class EnhancedRSIStrategyTest extends StrategyTestBase {

	private EnhancedRSIStrategy rsiStrategy;

	@Before
	public void setUp() {
		super.setUpBase();
		rsiStrategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		setupStrategyDependencies(rsiStrategy);
	}

	@Test
	public void testRSIStrategyInitialization() {
		assertNotNull("策略应该成功初始化", rsiStrategy);
		assertEquals("RSI策略", rsiStrategy.getName());
		assertTrue("策略应该已初始化", rsiStrategy.isInitialized());
		assertEquals("INITIALIZED", rsiStrategy.getStatus());
	}

	@Test
	public void testRSISignalGeneration() {
		// 使用测试数据中的第一个Bar
		BarEvent testBar = testBars.get(0);

		List<SignalEvent> signals = rsiStrategy.onBar(testBar);

		assertNotNull("信号列表不应为null", signals);
		// RSI策略可能根据计算决定是否生成信号
		System.out.println("生成的信号数量: " + signals.size());

		for (SignalEvent signal : signals) {
			assertNotNull("信号不应为null", signal);
			assertEquals("信号标的应该匹配", testBar.getSymbol(), signal.getSymbol());
			assertTrue("信号强度应该在0-1之间", signal.getStrength() >= 0 && signal.getStrength() <= 1);
		}
	}

	@Test
	public void testRSIParameters() {
		assertEquals(14, rsiStrategy.getRsiPeriod());
		assertEquals(70.0, rsiStrategy.getOverbought(), 0.001);
		assertEquals(30.0, rsiStrategy.getOversold(), 0.001);

		// 测试参数更新
		rsiStrategy.setRsiPeriod(21);
		rsiStrategy.setOverbought(75.0);
		rsiStrategy.setOversold(25.0);

		assertEquals(21, rsiStrategy.getRsiPeriod());
		assertEquals(75.0, rsiStrategy.getOverbought(), 0.001);
		assertEquals(25.0, rsiStrategy.getOversold(), 0.001);
	}

	@Test
	public void testRSIWithMultipleBars() {
		// 测试处理多个Bar的情况
		int signalCount = 0;

		for (int i = 0; i < Math.min(10, testBars.size()); i++) {
			BarEvent bar = testBars.get(i);
			List<SignalEvent> signals = rsiStrategy.onBar(bar);
			signalCount += signals.size();

			// 验证每个信号的基本属性
			for (SignalEvent signal : signals) {
				assertNotNull(signal.getTimestamp());
				assertNotNull(signal.getSymbol());
				assertNotNull(signal.getSignalType());
			}
		}

		System.out.println("处理10个Bar生成的信号总数: " + signalCount);
	}

	@Test
	public void testRSIDebugMode() {
		rsiStrategy.setDebugMode(true);
		assertTrue("调试模式应该启用", rsiStrategy.getParameters().containsKey("debugMode"));

		BarEvent testBar = testBars.get(0);
		List<SignalEvent> signals = rsiStrategy.onBar(testBar);

		assertNotNull(signals);
	}

	@Test
	public void testRSIStrategyStatus() {
		// 测试策略状态管理
		assertEquals("INITIALIZED", rsiStrategy.getStatus());

		rsiStrategy.reset();
		assertEquals("RESET", rsiStrategy.getStatus());

		// 重置后应该可以重新初始化
		rsiStrategy.initialize();
		assertEquals("INITIALIZED", rsiStrategy.getStatus());
	}
}